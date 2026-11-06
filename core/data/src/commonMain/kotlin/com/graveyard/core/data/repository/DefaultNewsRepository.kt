package com.graveyard.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.graveyard.core.data.local.preferences.requireValidTagInfos
import com.graveyard.core.data.mapper.toNewsBanner
import com.graveyard.core.data.mapper.toTagInfo
import com.graveyard.core.data.network.YingdiData
import com.graveyard.core.data.paging.ArticleCommentsPagingSource
import com.graveyard.core.data.paging.ArticleRepliesPagingSource
import com.graveyard.core.data.paging.NewsPagingSource
import com.graveyard.core.data.result.DataResult
import com.graveyard.core.data.result.toDataException
import com.graveyard.core.model.news.ArticleCommentOrder
import com.graveyard.core.model.news.ArticleCommentVote
import com.graveyard.core.model.news.ArticleCommentVoteAction
import com.graveyard.core.model.news.ArticleReplyOrder
import com.graveyard.core.model.news.NewsArticle
import com.graveyard.core.model.news.NewsBanner
import com.graveyard.core.model.news.TagInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withContext

internal class DefaultNewsRepository(
    private val remoteDataSource: YingdiData,
) : NewsRepository {
    override suspend fun getBanners(tagId: Int): DataResult<List<NewsBanner>> {
        return try {
            val response = remoteDataSource.getBannerList(tagId)
            DataResult.Success(response.topContent.map { it.toNewsBanner() })
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            DataResult.Failure(exception.toDataException().error)
        }
    }

    override fun getNews(
        tagId: Int,
        pageSize: Int,
    ): Flow<PagingData<NewsArticle>> {
        require(pageSize > 0) { "pageSize must be greater than zero" }

        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                initialLoadSize = pageSize,
                enablePlaceholders = false,
                prefetchDistance = (pageSize / 2).coerceAtLeast(1),
            ),
            initialKey = 1,
            pagingSourceFactory = {
                NewsPagingSource(
                    remoteDataSource = remoteDataSource,
                    tagId = tagId,
                    pageSize = pageSize,
                )
            },
        ).flow
    }

    override suspend fun refreshTagInfos(): DataResult<List<TagInfo>> {
        return try {
            val response = remoteDataSource.getTagList()
            val tags = withContext(Dispatchers.Default) {
                response.map { it.toTagInfo() }.also(::requireValidTagInfos)
            }
            currentCoroutineContext().ensureActive()
            DataResult.Success(tags)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            DataResult.Failure(exception.toDataException().error)
        }
    }

    override suspend fun getArticleContent(postId: Long): DataResult<ArticleContent> {
        return try {
            require(postId > 0) { "postId must be positive" }
            val response = remoteDataSource.getArticleContentHtml(postId)
            currentCoroutineContext().ensureActive()
            DataResult.Success(
                ArticleContent(
                    articleId = postId,
                    html = response.html,
                    baseUrl = "https://mob.iyingdi.com/post/$postId",
                    newsAuthor = response.newsAuthor,
                    replyNum = response.replyNum,
                    likeNum = response.likeNum,
                    hateNum = response.hateNum,
                    viewer = response.viewer,
                ),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            DataResult.Failure(exception.toDataException().error)
        }
    }

    override suspend fun setArticleVote(postId: Long, vote: ArticleVote, userId: Long): DataResult<Unit> = interaction {
        require(postId > 0) { "postId must be positive" }
        remoteDataSource.setArticleVote(postId, when (vote) {
            ArticleVote.None -> 0
            ArticleVote.Like -> 1
            ArticleVote.Hate -> 2
        }, expectedUserId = userId)
    }

    override suspend fun toggleArticleCollect(postId: Long, userId: Long): DataResult<Unit> = interaction {
        require(postId > 0) { "postId must be positive" }
        remoteDataSource.toggleArticleCollect(postId, expectedUserId = userId)
    }

    override suspend fun setCommentVote(
        postId: Long,
        commentId: Long,
        action: ArticleCommentVoteAction,
        userId: Long,
    ): DataResult<Unit> = interaction {
        require(postId > 0) { "postId must be positive" }
        require(commentId > 0) { "commentId must be positive" }
        val currentUserId = userId.takeIf { it > 0 } ?: throw IllegalArgumentException("userId must be positive")
        val requestAction = when (action) {
            ArticleCommentVoteAction.Like -> 1
            ArticleCommentVoteAction.CancelLike -> -1
            ArticleCommentVoteAction.Dislike -> 2
            ArticleCommentVoteAction.CancelDislike -> -2
        }
        remoteDataSource.setCommentLike(postId, commentId, requestAction, expectedUserId = currentUserId)
    }

    private suspend fun interaction(block: suspend () -> Unit): DataResult<Unit> = try {
        block()
        currentCoroutineContext().ensureActive()
        DataResult.Success(Unit)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        currentCoroutineContext().ensureActive()
        DataResult.Failure(exception.toDataException().error)
    }

    override fun getArticleComments(
        postId: Long,
        order: ArticleCommentOrder,
        pageSize: Int,
    ): ArticleCommentsFeed {
        require(pageSize > 0) { "pageSize must be greater than zero" }

        val state = MutableStateFlow(ArticleCommentsFeedState())
        val comments = Pager(
            config = PagingConfig(
                pageSize = pageSize,
                initialLoadSize = pageSize,
                enablePlaceholders = false,
                prefetchDistance = 10,
            ),
            initialKey = 0L,
            pagingSourceFactory = {
                val generation = state.updateAndGet { current ->
                    current.copy(
                        generation = current.generation + 1,
                    )
                }.generation
                ArticleCommentsPagingSource(
                    remoteDataSource = remoteDataSource,
                    postId = postId,
                    order = order,
                    pageSize = pageSize,
                    onFirstPageLoaded = { metadata ->
                        // A refresh must keep the previous first page until its replacement succeeds.
                        state.update { current ->
                            if (current.generation == generation) current.copy(metadata = metadata) else current
                        }
                    },
                )
            },
        ).flow

        return ArticleCommentsFeed(
            comments = comments,
            state = state.asStateFlow(),
        )
    }

    override fun getArticleReplies(
        commentId: Long,
        order: ArticleReplyOrder,
        pageSize: Int,
    ): ArticleRepliesFeed {
        require(pageSize > 0) { "pageSize must be greater than zero" }

        val state = MutableStateFlow(ArticleRepliesFeedState())
        val replies = Pager(
            config = PagingConfig(
                pageSize = pageSize,
                initialLoadSize = pageSize,
                enablePlaceholders = false,
                prefetchDistance = (pageSize / 2).coerceAtLeast(1),
            ),
            initialKey = 0L,
            pagingSourceFactory = {
                val generation = state.updateAndGet { current ->
                    current.copy(generation = current.generation + 1)
                }.generation
                ArticleRepliesPagingSource(
                    remoteDataSource = remoteDataSource,
                    commentId = commentId,
                    order = order,
                    pageSize = pageSize,
                    onPageLoaded = { metadata ->
                        state.update { current ->
                            if (current.generation == generation) current.copy(metadata = metadata) else current
                        }
                    },
                )
            },
        ).flow

        return ArticleRepliesFeed(
            replies = replies,
            state = state.asStateFlow(),
        )
    }
}
