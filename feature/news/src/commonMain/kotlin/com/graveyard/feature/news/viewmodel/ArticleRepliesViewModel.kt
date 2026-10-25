package com.graveyard.feature.news.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.graveyard.core.data.repository.ArticleRepliesFeed
import com.graveyard.core.data.repository.NewsRepository
import com.graveyard.core.data.result.DataError
import com.graveyard.core.model.news.ArticleComment
import com.graveyard.core.model.news.ArticleRepliesMetadata
import com.graveyard.core.model.news.ArticleReplyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArticleRepliesViewModel(
    private val repository: NewsRepository,
    private val commentId: Long,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        ArticleRepliesUiState(
            order = ArticleReplyOrder.entries.firstOrNull {
                it.name == savedStateHandle.get<String>(ORDER_KEY)
            } ?: ArticleReplyOrder.Descending,
        ),
    )
    val uiState: StateFlow<ArticleRepliesUiState> = mutableUiState.asStateFlow()
    private val replySessions = MutableStateFlow<Flow<PagingData<ArticleComment>>>(emptyFlow())
    @OptIn(ExperimentalCoroutinesApi::class)
    val replies = replySessions.flatMapLatest { it }
    private var sessionJob: Job? = null

    init { loadReplies() }

    fun loadReplies() {
        if (mutableUiState.value.feed != null) return
        startRepliesSession(mutableUiState.value.order)
    }

    private fun startRepliesSession(order: ArticleReplyOrder) {
        if (commentId <= 0L) {
            mutableUiState.update { it.copy(error = DataError.Parsing) }
            return
        }
        val feed = repository.getArticleReplies(commentId, order, pageSize = 10)
        val job = SupervisorJob(viewModelScope.coroutineContext[Job])
        val scope = CoroutineScope(viewModelScope.coroutineContext + job)
        val sessionFeed = feed.copy(
            replies = feed.replies.map { data -> data.filter { it.isVisible } }.cachedIn(scope),
        )
        val previousSession = sessionJob
        sessionJob = job
        mutableUiState.update { it.copy(order = order, feed = sessionFeed, error = null) }
        replySessions.value = sessionFeed.replies
        previousSession?.cancel()
        scope.launch {
            feed.state.map { it.metadata }.filterNotNull().distinctUntilChanged().collect { metadata ->
                mutableUiState.update { state ->
                    if (state.feed === sessionFeed) state.copy(metadata = metadata) else state
                }
            }
        }
    }

    fun selectOrder(order: ArticleReplyOrder) {
        if (mutableUiState.value.order == order) return
        savedStateHandle[ORDER_KEY] = order.name
        startRepliesSession(order)
    }

    fun onLikeClicked(commentId: Long, parentCommentId: Long) {
        // TODO: Connect the comment like endpoint.
    }

    fun onDislikeClicked(commentId: Long, parentCommentId: Long) {
        // TODO: Connect the comment dislike endpoint.
    }

    fun onMoreClicked(commentId: Long, parentCommentId: Long) {
        // TODO: Connect the comment actions menu.
    }

    private companion object {
        const val ORDER_KEY = "articleReplies.order"
    }
}

data class ArticleRepliesUiState(
    val order: ArticleReplyOrder = ArticleReplyOrder.Descending,
    val feed: ArticleRepliesFeed? = null,
    /** Latest successful root comment; preserved when refreshing or changing order. */
    val metadata: ArticleRepliesMetadata? = null,
    val error: DataError? = null,
)
