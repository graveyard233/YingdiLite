package com.graveyard.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.graveyard.core.data.mapper.toArticleCommentThread
import com.graveyard.core.data.network.YingdiData
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException
import com.graveyard.core.data.result.toDataException
import com.graveyard.core.model.news.ArticleCommentOrder
import com.graveyard.core.model.news.ArticleCommentThread
import com.graveyard.core.model.news.ArticleCommentsMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class ArticleCommentsPagingSource(
    private val remoteDataSource: YingdiData,
    private val postId: Long,
    private val order: ArticleCommentOrder,
    private val pageSize: Int,
    private val onFirstPageLoaded: (ArticleCommentsMetadata) -> Unit,
) : PagingSource<Long, ArticleCommentThread>() {
    private val loadedCommentIds = linkedSetOf<Long>()
    private val requestedCursors = linkedSetOf(0L)
    private val loadMutex = Mutex()

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, ArticleCommentThread> =
        loadMutex.withLock { loadPage(params) }

    private suspend fun loadPage(params: LoadParams<Long>): LoadResult<Long, ArticleCommentThread> {
        val cursor = params.key ?: 0L
        return try {
            if (postId <= 0L || pageSize <= 0) throw DataException(DataError.Parsing)
            if (cursor !in requestedCursors) throw DataException(DataError.Parsing)

            val response = remoteDataSource.getArticleCommentList(
                postId = postId,
                lastCommentId = cursor,
                order = order.apiValue,
                size = pageSize,
                voteFaction = 0,
            )
            currentCoroutineContext().ensureActive()

            val rawNormalComments = response.normalComments.orEmpty()
            // Read the opaque cursor before mapping or removing duplicate ordinary comments.
            val nextCursor = rawNormalComments.lastOrNull()?.mainComment?.comment?.id
            val isFirstPage = cursor == 0L
            val pageIds = rawNormalComments.map { it.mainComment.comment.id }
            if (pageIds.any { it <= 0L } || response.normalNum < 0 || (isFirstPage && response.hotNum < 0)) {
                throw DataException(DataError.Parsing)
            }
            val newIds = pageIds.filterNot(loadedCommentIds::contains).toSet()
            val loadedCount = loadedCommentIds.size + newIds.size
            val normalTotal = response.normalNum
            // An empty page ends pagination even when the reported total exceeds loaded IDs.
            val hasMore = rawNormalComments.isNotEmpty() && loadedCount < normalTotal

            if (hasMore && (nextCursor == null || nextCursor <= 0L || nextCursor == cursor)) {
                throw DataException(DataError.Parsing)
            }
            if (hasMore && nextCursor != null && nextCursor in requestedCursors) {
                throw DataException(DataError.Parsing)
            }

            // Map the whole result before committing counters, so a failed page can be retried.
            val (pageData, metadata) = withContext(Dispatchers.Default) {
                val data = rawNormalComments
                    .filter { it.mainComment.comment.id !in loadedCommentIds }
                    .distinctBy { it.mainComment.comment.id }
                    .map { it.toArticleCommentThread() }
                val firstPageMetadata = if (isFirstPage) {
                    val hot = if (order == ArticleCommentOrder.Hot) {
                        response.hotComments.orEmpty().distinctBy { it.mainComment.comment.id }
                            .map { it.toArticleCommentThread() }
                    } else emptyList()
                    ArticleCommentsMetadata(hot, response.hotNum, normalTotal)
                } else null
                data to firstPageMetadata
            }

            currentCoroutineContext().ensureActive()
            if (invalid) return LoadResult.Invalid()
            val result = LoadResult.Page(
                data = pageData,
                prevKey = null,
                nextKey = nextCursor.takeIf { hasMore },
            )
            loadedCommentIds += newIds
            if (hasMore) requestedCursors += requireNotNull(nextCursor)
            if (metadata != null) onFirstPageLoaded(metadata)
            result
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            LoadResult.Error(exception.toDataException())
        }
    }

    override fun getRefreshKey(state: PagingState<Long, ArticleCommentThread>): Long? = null
}
