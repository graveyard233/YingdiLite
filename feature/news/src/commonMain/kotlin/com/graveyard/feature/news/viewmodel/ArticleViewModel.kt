package com.graveyard.feature.news.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.data.repository.AuthRepository
import com.graveyard.core.data.repository.ArticleContent
import com.graveyard.core.data.repository.ArticleVote
import com.graveyard.core.data.repository.ArticleCommentsFeed
import com.graveyard.core.data.repository.NewsRepository
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataResult
import com.graveyard.core.designsystem.webview.RichContentReadingState
import com.graveyard.core.model.news.ArticleCommentOrder
import com.graveyard.core.model.news.ArticleCommentVote
import com.graveyard.core.model.news.ArticleCommentThread
import com.graveyard.core.model.news.ArticleCommentsMetadata
import com.graveyard.core.model.news.NewsAuthor
import com.graveyard.feature.news.navigation.ArticleAuthAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ArticleViewModel internal constructor(
    private val repository: NewsRepository,
    private val authRepository: AuthRepository,
    private val commentVoteCoordinator: CommentVoteCoordinator,
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
    private var interactionJob: Job? = null
    private var voteCooldownJob: Job? = null
    private var interactionGeneration = 0L
    private var feedbackId = 0L
    private var interactionIdentity = currentUserId()
    private val commentSnapshots = mutableMapOf<Long, com.graveyard.core.model.news.ArticleComment>()

    init {
        viewModelScope.launch {
            authRepository.state.map { (it as? AuthState.Authenticated)?.userId }.distinctUntilChanged().collect {
                updateInteractionIdentity(currentUserId())
            }
        }
        loadArticle()
    }

    fun loadArticle() {
        request?.cancel()
        mutableUiState.update { it.copy(isLoading = true, error = null) }
        request = viewModelScope.launch {
            val viewerUserId = currentUserId()
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
            if (result is DataResult.Success && viewerUserId != null && viewerUserId == currentUserId() &&
                !uiState.value.interaction.isBusy) {
                applyInteraction(result.data, viewerUserId)
            }
        }
    }

    private fun currentUserId(): Long? = (authRepository.state.value as? AuthState.Authenticated)?.userId

    private fun updateInteractionIdentity(userId: Long?) {
        if (interactionIdentity == userId) return
        interactionIdentity = userId
        interactionGeneration++
        voteCooldownJob?.cancel()
        // A request that invalidates its own session must return its explicit auth error first.
        // A normal logout still invalidates its generation, so it cannot start another request.
        if (userId != null) interactionJob?.cancel()
        mutableUiState.update {
            it.copy(
                interaction = ArticleInteractionState(),
                isVoteCoolingDown = false,
                interactionFeedback = null,
            )
        }
    }

    fun syncInteraction() {
        val userId = currentUserId() ?: return
        if (uiState.value.interaction.let { it.userId == userId && it.viewer != null && it.isSynchronized }) return
        startInteraction(null, userId)
    }

    /** The route claims a saved result only after this VM can accept it. */
    fun canAcceptInteraction(userId: Long): Boolean = currentUserId() == userId &&
        !uiState.value.isLoading && uiState.value.content != null && !uiState.value.interaction.isBusy

    fun performInteraction(action: ArticleAuthAction, userId: Long) {
        if (action.postId != articleId.toLongOrNull()) return
        if (action is ArticleAuthAction.SetCommentVote) {
            voteComment(action.commentId, action.vote, userId)
            return
        }
        startInteraction(action, userId)
    }

    fun commentVoteStates(userId: Long?) = commentVoteCoordinator.observeArticle(articleId.toLongOrNull() ?: 0L, userId)

    fun onCommentLikeClicked(comment: com.graveyard.core.model.news.ArticleComment, parentCommentId: Long) =
        voteComment(comment, ArticleCommentVote.Like, currentUserId())

    fun onCommentDislikeClicked(comment: com.graveyard.core.model.news.ArticleComment, parentCommentId: Long) =
        voteComment(comment, ArticleCommentVote.Dislike, currentUserId())

    fun voteComment(commentId: Long, vote: ArticleCommentVote, userId: Long?) {
        val comment = commentSnapshots[commentId] ?: findComment(commentId) ?: return
        voteComment(comment, vote, userId)
    }

    fun cacheCommentSnapshot(comment: com.graveyard.core.model.news.ArticleComment) {
        commentSnapshots[comment.id] = comment
    }

    private fun voteComment(comment: com.graveyard.core.model.news.ArticleComment, vote: ArticleCommentVote, userId: Long?) {
        val postId = articleId.toLongOrNull()?.takeIf { it > 0L } ?: return
        val authenticatedUserId = userId?.takeIf { it == currentUserId() } ?: return
        commentVoteCoordinator.vote(postId, authenticatedUserId, comment, vote)
    }

    private fun findComment(commentId: Long): com.graveyard.core.model.news.ArticleComment? {
        commentsState.value.metadata?.hotComments?.forEach { thread ->
            if (thread.mainComment.id == commentId) return thread.mainComment
            thread.replies.firstOrNull { it.id == commentId }?.let { return it }
        }
        return null
    }

    fun consumeInteractionFeedback(id: Long): ArticleInteractionFeedback? {
        val feedback = uiState.value.interactionFeedback?.takeIf { it.id == id } ?: return null
        mutableUiState.update { it.copy(interactionFeedback = null) }
        return feedback
    }

    private fun startVoteCooldown() {
        voteCooldownJob?.cancel()
        mutableUiState.update { it.copy(isVoteCoolingDown = true) }
        voteCooldownJob = viewModelScope.launch {
            delay(5_000L)
            mutableUiState.update { it.copy(isVoteCoolingDown = false) }
        }
    }

    private fun startInteraction(action: ArticleAuthAction?, userId: Long) {
        // A result can arrive before the auth-state collector resumes after login.
        updateInteractionIdentity(currentUserId())
        if (!canAcceptInteraction(userId)) return
        if (action is ArticleAuthAction.SetVote && uiState.value.isVoteCoolingDown) return
        val generation = ++interactionGeneration
        mutableUiState.update { it.copy(interaction = it.interaction.copy(isBusy = true)) }
        interactionJob = viewModelScope.launch {
            fun checkIdentity() {
                if (generation != interactionGeneration || currentUserId() != userId) throw CancellationException("Article account changed")
            }
            try {
                val cached = uiState.value.interaction.takeIf {
                    it.userId == userId && it.viewer != null && it.isSynchronized
                }
                if (cached == null) {
                    when (val synced = readInteraction(userId)) {
                        is DataResult.Success -> {
                            checkIdentity()
                            applyInteraction(synced.data, userId)
                        }
                        is DataResult.Failure -> {
                            handleAuthFailure(synced.error, action, userId, generation)
                            checkIdentity()
                            reportInteraction(ArticleInteractionMessage.SyncFailed, synced.error)
                            return@launch
                        }
                    }
                }
                checkIdentity()
                if (action == null) return@launch
                if (action is ArticleAuthAction.SetCommentVote) return@launch
                val before = uiState.value.interaction
                val viewer = before.viewer ?: return@launch
                val changed = when (action) {
                    is ArticleAuthAction.SetVote -> viewer.vote != action.vote
                    is ArticleAuthAction.SetCollected -> viewer.collected != action.collected
                }
                if (!changed) return@launch
                val result = when (action) {
                    is ArticleAuthAction.SetVote -> {
                        startVoteCooldown()
                        repository.setArticleVote(action.postId, action.vote, userId)
                    }
                    is ArticleAuthAction.SetCollected -> repository.toggleArticleCollect(action.postId, userId)
                }
                if (result is DataResult.Failure) handleAuthFailure(result.error, action, userId, generation)
                checkIdentity()
                when (result) {
                    is DataResult.Success -> {
                        val updated = when (action) {
                            is ArticleAuthAction.SetVote -> before.copy(
                                viewer = viewer.copy(vote = action.vote, rawAction = when (action.vote) {
                                    ArticleVote.None -> 0
                                    ArticleVote.Like -> 1
                                    ArticleVote.Hate -> 2
                                }),
                                likeNum = before.likeNum.adjustCount((if (action.vote == ArticleVote.Like) 1 else 0) -
                                    (if (viewer.vote == ArticleVote.Like) 1 else 0)),
                                hateNum = before.hateNum.adjustCount((if (action.vote == ArticleVote.Hate) 1 else 0) -
                                    (if (viewer.vote == ArticleVote.Hate) 1 else 0)),
                            )
                            is ArticleAuthAction.SetCollected -> before.copy(viewer = viewer.copy(collected = action.collected))
                        }
                        mutableUiState.update { it.copy(interaction = updated) }
                    }
                    is DataResult.Failure -> {
                        // An ambiguous response must never cause an automatic second toggle.
                        mutableUiState.update { it.copy(interaction = it.interaction.copy(isSynchronized = false)) }
                        reportInteraction(ArticleInteractionMessage.OperationFailed, result.error)
                    }
                }
                // 注释部分的代码是执行完请求后和服务端对其数据的静默重读逻辑，目前不需要，先注释
//                when (val refreshed = readInteraction(userId)) {
//                    is DataResult.Success -> {
//                        checkIdentity()
//                        applyInteraction(refreshed.data, userId)
//                    }
//                    is DataResult.Failure -> {
//                        checkIdentity()
//                        if (result is DataResult.Success) reportInteraction(ArticleInteractionMessage.SavedSyncFailed, refreshed.error)
//                    }
//                }
            } finally {
                if (generation == interactionGeneration) {
                    mutableUiState.update { it.copy(interaction = it.interaction.copy(isBusy = false)) }
                }
            }
        }
    }

    private suspend fun handleAuthFailure(
        error: DataError,
        action: ArticleAuthAction?,
        userId: Long,
        generation: Long,
    ) {
        if (action == null) return
        val code = (error as? DataError.Business)?.retCode
        if (code != "200022" && code != "200002") return
        if (code == "200022") {
            // Token refresher has cleared the session; wait for AuthRepository to publish it.
            withTimeoutOrNull(1_000L) {
                authRepository.state.first { it !is AuthState.Authenticated || it.userId != userId }
            }
        }
        val sameAttempt = generation == interactionGeneration ||
            (generation + 1 == interactionGeneration && interactionIdentity == null)
        if (sameAttempt && authRepository.state.value == AuthState.Guest) {
            updateInteractionIdentity(null)
            mutableUiState.update {
                it.copy(interactionFeedback = ArticleInteractionFeedback(++feedbackId, requiresLogin = action))
            }
        }
    }

    private suspend fun readInteraction(userId: Long): DataResult<ArticleContent> {
        suspend fun read(): DataResult<ArticleContent> {
            if (currentUserId() != userId) throw CancellationException("Article account changed")
            return repository.getArticleContent(articleId.toLong())
        }
        val first = read()
        when (first) {
            is DataResult.Failure -> return first
            is DataResult.Success -> if (first.data.viewer != null) return first
        }
        // SSR silently serves a guest page on expired tokens. Exercise the existing refresh path once.
        if (currentUserId() != userId) throw CancellationException("Article account changed")
        when (val validation = authRepository.refreshCurrentUser()) {
            is DataResult.Failure -> return validation
            is DataResult.Success -> Unit
        }
        return when (val second = read()) {
            is DataResult.Failure -> second
            is DataResult.Success -> if (second.data.viewer == null) DataResult.Failure(DataError.Parsing) else second
        }
    }

    private fun applyInteraction(content: ArticleContent, userId: Long) {
        if (currentUserId() != userId) return
        updateInteractionIdentity(userId)
        mutableUiState.update {
            it.copy(interaction = ArticleInteractionState(
                userId = userId, viewer = content.viewer, likeNum = content.likeNum, hateNum = content.hateNum,
                isSynchronized = content.viewer != null,
                isBusy = it.interaction.isBusy,
            ))
        }
    }

    private fun reportInteraction(message: ArticleInteractionMessage, error: DataError) {
        mutableUiState.update { it.copy(interactionFeedback = ArticleInteractionFeedback(++feedbackId, message, error)) }
    }

    private fun String.adjustCount(delta: Int): String = toLongOrNull()?.let {
        (it + delta).coerceAtLeast(0).toString()
    } ?: this

    fun loadComments() {
        if (mutableCommentsState.value.feed != null) return
        val postId = articleId.toLongOrNull()?.takeIf { it > 0L }
        if (postId == null) {
            mutableCommentsState.update { it.copy(error = DataError.Parsing) }
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
        val sessionFeed = feed.copy(
            comments = feed.comments
                .map { pagingData ->
                    pagingData
                        .filter { it.mainComment.isVisible }
                        .map { it.withVisibleReplies() }
                }
                .cachedIn(sessionScope),
        )
        mutableCommentsState.update { it.copy(feed = sessionFeed, metadata = null, error = null) }
        // The first-page metadata travels next to the paging data, so the UI never has to look
        // inside the list for it. A failed refresh keeps the last successful metadata.
        sessionScope.launch {
            feed.state.map { it.metadata }.distinctUntilChanged().collect { rawMetadata ->
                val metadata = rawMetadata?.let {
                    it.copy(
                        hotComments = it.hotComments
                            .filter { thread -> thread.mainComment.isVisible }
                            .map { thread -> thread.withVisibleReplies() },
                    )
                }
                mutableCommentsState.update { current ->
                    if (current.feed === sessionFeed) current.copy(metadata = metadata) else current
                }
            }
        }
    }

    private fun ArticleCommentThread.withVisibleReplies(): ArticleCommentThread {
        val visibleReplies = replies.filter { it.isVisible }
        val hiddenReplyCount = replies.size - visibleReplies.size
        if (hiddenReplyCount == 0) return this
        return copy(
            mainComment = mainComment.copy(
                replyNum = (mainComment.replyNum - hiddenReplyCount).coerceAtLeast(0),
            ),
            replies = visibleReplies,
        )
    }

    fun selectCommentOrder(order: ArticleCommentOrder) {
        if (mutableCommentsState.value.order == order) return
        commentsSessionJob?.cancel()
        commentsSessionJob = null
        mutableCommentsState.update { it.copy(order = order, feed = null, metadata = null, error = null) }
        loadComments()
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
    /** Failure to create a session; request failures are reported by Paging. */
    val error: DataError? = null,
)
