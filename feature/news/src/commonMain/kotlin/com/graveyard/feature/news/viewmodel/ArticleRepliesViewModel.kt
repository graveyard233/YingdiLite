package com.graveyard.feature.news.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.graveyard.core.data.repository.ArticleRepliesFeed
import com.graveyard.core.data.repository.NewsRepository
import com.graveyard.core.data.repository.AuthRepository
import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.data.result.DataError
import com.graveyard.core.model.news.ArticleComment
import com.graveyard.core.model.news.ArticleCommentVote
import com.graveyard.core.model.news.ArticleRepliesMetadata
import com.graveyard.core.model.news.ArticleReplyOrder
import com.graveyard.feature.news.navigation.ArticleAuthAction
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

class ArticleRepliesViewModel internal constructor(
    private val repository: NewsRepository,
    private val commentVoteCoordinator: CommentVoteCoordinator,
    private val authRepository: AuthRepository,
    private val articleId: String,
    private val commentId: Long,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val commentSnapshots = mutableMapOf<Long, ArticleComment>()
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
    fun commentVoteStates(userId: Long?) = commentVoteCoordinator.observeArticle(articleId.toLongOrNull() ?: 0L, userId)
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

    fun onLikeClicked(comment: ArticleComment, parentCommentId: Long) = vote(comment, ArticleCommentVote.Like)

    fun onDislikeClicked(comment: ArticleComment, parentCommentId: Long) = vote(comment, ArticleCommentVote.Dislike)

    fun vote(comment: ArticleComment, target: ArticleCommentVote, userId: Long? = currentUserId()) {
        cacheCommentSnapshot(comment)
        val postId = articleId.toLongOrNull()?.takeIf { it > 0L } ?: return
        val authenticatedUserId = userId?.takeIf { it == currentUserId() } ?: return
        commentVoteCoordinator.vote(postId, authenticatedUserId, comment, target)
    }

    fun cacheCommentSnapshot(comment: ArticleComment) { commentSnapshots[comment.id] = comment }

    fun canAcceptInteraction(userId: Long): Boolean = currentUserId() == userId

    fun performInteraction(action: ArticleAuthAction, userId: Long) {
        if (action !is ArticleAuthAction.SetCommentVote) return
        if (action.postId != articleId.toLongOrNull() || currentUserId() != userId) return
        val comment = commentSnapshots[action.commentId] ?: return
        vote(comment, action.vote, userId)
    }

    private fun currentUserId() = (authRepository.state.value as? AuthState.Authenticated)?.userId

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
