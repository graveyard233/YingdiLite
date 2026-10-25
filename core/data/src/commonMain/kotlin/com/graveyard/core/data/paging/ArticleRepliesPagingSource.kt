package com.graveyard.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.graveyard.core.data.mapper.toArticleCommentThread
import com.graveyard.core.data.network.YingdiData
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException
import com.graveyard.core.data.result.toDataException
import com.graveyard.core.model.news.ArticleComment
import com.graveyard.core.model.news.ArticleRepliesMetadata
import com.graveyard.core.model.news.ArticleReplyOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class ArticleRepliesPagingSource(
    private val remoteDataSource: YingdiData,
    private val commentId: Long,
    private val order: ArticleReplyOrder,
    private val pageSize: Int,
    private val onPageLoaded: (ArticleRepliesMetadata) -> Unit,
) : PagingSource<Long, ArticleComment>() {
    private val loadedReplyIds = linkedSetOf<Long>()
    private val requestedCursors = linkedSetOf(0L)
    private val loadMutex = Mutex()

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, ArticleComment> =
        loadMutex.withLock { loadPage(params) }

    private suspend fun loadPage(params: LoadParams<Long>): LoadResult<Long, ArticleComment> {
        val cursor = params.key ?: 0L
        return try {
            if (commentId <= 0L || pageSize <= 0 || cursor !in requestedCursors) {
                throw DataException(DataError.Parsing)
            }
            val response = remoteDataSource.getCommentMore(
                commentId = commentId,
                lastReplyId = cursor,
                order = order.apiValue,
                size = pageSize,
            )
            currentCoroutineContext().ensureActive()

            val rawReplies = response.replyComments.orEmpty()
            // Both the cursor and the short-page check use the unfiltered response.
            val nextCursor = rawReplies.lastOrNull()?.comment?.id
            val pageIds = rawReplies.map { it.comment.id }
            if (response.mainComment.comment.id != commentId || pageIds.any { it <= 0L }) {
                throw DataException(DataError.Parsing)
            }
            val hasMore = rawReplies.isNotEmpty() && rawReplies.size >= pageSize &&
                nextCursor != null && nextCursor !in requestedCursors

            // Commit IDs only after mapping succeeds, so failed pages can be retried.
            val (pageData, metadata) = withContext(Dispatchers.Default) {
                val thread = response.copy(
                    replyComments = rawReplies
                        .filter { it.comment.id !in loadedReplyIds }
                        .distinctBy { it.comment.id },
                ).toArticleCommentThread()
                thread.replies to ArticleRepliesMetadata(thread.mainComment, thread.isManager)
            }

            currentCoroutineContext().ensureActive()
            if (invalid) return LoadResult.Invalid()
            val result = LoadResult.Page(
                data = pageData,
                prevKey = null,
                nextKey = nextCursor.takeIf { hasMore },
            )
            loadedReplyIds += pageIds
            if (hasMore) requestedCursors += requireNotNull(nextCursor)
            onPageLoaded(metadata)
            result
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            LoadResult.Error(exception.toDataException())
        }
    }

    override fun getRefreshKey(state: PagingState<Long, ArticleComment>): Long? = null
}
