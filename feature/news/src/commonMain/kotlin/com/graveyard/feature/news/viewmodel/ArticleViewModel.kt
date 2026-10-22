package com.graveyard.feature.news.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.graveyard.core.data.repository.ArticleCommentsFeed
import com.graveyard.core.data.repository.NewsRepository
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataResult
import com.graveyard.core.designsystem.webview.RichContentReadingState
import com.graveyard.core.model.news.ArticleCommentOrder
import com.graveyard.core.model.news.ArticleCommentsMetadata
import com.graveyard.core.model.news.NewsAuthor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArticleViewModel(
    private val repository: NewsRepository,
    private val articleId: String,
    articleTitle: String,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val readingState = RichContentReadingState(
        savedSnapshot = savedStateHandle[READING_POSITION_KEY],
        onSnapshotChanged = { savedStateHandle[READING_POSITION_KEY] = it },
    )
    private val mutableUiState = MutableStateFlow(
        ArticleUiState(
            articleId = articleId,
            articleTitle = articleTitle,
        ),
    )
    val uiState: StateFlow<ArticleUiState> = mutableUiState.asStateFlow()
    private val mutableCommentsState = MutableStateFlow(ArticleCommentsUiState())
    val commentsState: StateFlow<ArticleCommentsUiState> = mutableCommentsState.asStateFlow()
    private var request: Job? = null
    private var commentsSessionJob: Job? = null

    init { loadArticle() }

    fun loadArticle() {
        request?.cancel()
        mutableUiState.update { it.copy(isLoading = true, error = null) }
        request = viewModelScope.launch {
            val id = articleId.toLongOrNull()?.takeIf { it > 0 }
            val result = if (id == null) DataResult.Failure(DataError.Parsing) else repository.getArticleContent(id)
            currentCoroutineContext().ensureActive()
            mutableUiState.update {
                when (result) {
                    is DataResult.Success -> it.copy(
                        content = result.data,
                        newsAuthor = result.data.newsAuthor,
                        isLoading = false,
                        error = null,
                    )
                    is DataResult.Failure -> it.copy(
                        content = null,
                        newsAuthor = NewsAuthor(),
                        isLoading = false,
                        error = result.error,
                    )
                }
            }
        }
    }

    fun loadComments() {
        if (mutableCommentsState.value.feed != null) return
        val postId = articleId.toLongOrNull()?.takeIf { it > 0L }
        if (postId == null) {
            mutableUiState.update { it.copy(commentsError = DataError.Parsing) }
            return
        }
        val feed = repository.getArticleComments(
            postId = postId,
            order = mutableCommentsState.value.order,
        )
        commentsSessionJob?.cancel()
        val sessionJob = SupervisorJob(viewModelScope.coroutineContext[Job])
        commentsSessionJob = sessionJob
        val sessionScope = CoroutineScope(viewModelScope.coroutineContext + sessionJob)
        mutableUiState.update { it.copy(commentsError = null) }
        val sessionFeed = feed.copy(comments = feed.comments.cachedIn(sessionScope))
        mutableCommentsState.update { it.copy(feed = sessionFeed, metadata = null) }
        // The first-page metadata travels next to the paging data, so the UI never has to look
        // inside the list for it. A failed refresh keeps the last successful metadata.
        sessionScope.launch {
            feed.state.collect { feedState ->
                mutableCommentsState.update { current ->
                    if (current.feed === sessionFeed) current.copy(metadata = feedState.metadata) else current
                }
            }
        }
    }

    fun selectCommentOrder(order: ArticleCommentOrder) {
        if (mutableCommentsState.value.order == order) return
        commentsSessionJob?.cancel()
        commentsSessionJob = null
        mutableCommentsState.update { it.copy(order = order, feed = null) }
        loadComments()
    }

    fun onCommentLikeClicked(commentId: Long, parentCommentId: Long) {
        // TODO: Connect the comment like endpoint.
    }

    fun onCommentDislikeClicked(commentId: Long, parentCommentId: Long) {
        // TODO: Connect the comment dislike endpoint.
    }

    fun onCommentMoreClicked(commentId: Long, parentCommentId: Long) {
        // TODO: Connect the comment actions menu.
    }

    fun onMoreClicked() {
        // Reserved for the article actions menu.
    }

    private companion object {
        const val READING_POSITION_KEY = "article.readingPosition"
    }
}

data class ArticleCommentsUiState(
    val order: ArticleCommentOrder = ArticleCommentOrder.Hot,
    val feed: ArticleCommentsFeed? = null,
    /** Latest first page of [feed]; kept while a refresh is loading or after it failed. */
    val metadata: ArticleCommentsMetadata? = null,
)
