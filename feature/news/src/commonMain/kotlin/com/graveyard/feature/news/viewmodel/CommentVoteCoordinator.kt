package com.graveyard.feature.news.viewmodel

import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.data.repository.AuthRepository
import com.graveyard.core.data.repository.NewsRepository
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataResult
import com.graveyard.core.model.news.ArticleComment
import com.graveyard.core.model.news.ArticleCommentVote
import com.graveyard.core.model.news.ArticleCommentVoteAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal data class CommentVoteKey(val postId: Long, val userId: Long, val commentId: Long)

data class CommentVoteUiState(
    val vote: ArticleCommentVote,
    val likeNum: Int,
    val hateNum: Int,
    val isPending: Boolean = false,
    val cooldownUntilMillis: Long = 0L,
    val feedbackId: Long = 0L,
    val error: DataError? = null,
) {
    @OptIn(ExperimentalTime::class)
    val isCoolingDown: Boolean get() = cooldownUntilMillis > nowMillis()
}

/** Shares a comment's confirmed vote and cooldown across the article and reply destinations. */
internal class CommentVoteCoordinator(
    private val repository: NewsRepository,
    private val authRepository: AuthRepository,
) {
    private val coordinatorScope = CoroutineScope(SupervisorJob())
    private val mutableStates = MutableStateFlow<Map<CommentVoteKey, CommentVoteUiState>>(emptyMap())
    private val requestJobs = mutableMapOf<CommentVoteKey, Job>()
    private val cooldownJobs = mutableMapOf<CommentVoteKey, Job>()
    private var feedbackId = 0L

    internal val states = mutableStates.asStateFlow()

    fun observeArticle(postId: Long, userId: Long?): Flow<Map<Long, CommentVoteUiState>> =
        states.map { all ->
            if (userId == null) emptyMap() else all
                .filterKeys { it.postId == postId && it.userId == userId }
                .mapKeys { it.key.commentId }
        }

    @OptIn(ExperimentalTime::class)
    fun vote(
        postId: Long,
        userId: Long,
        comment: ArticleComment,
        target: ArticleCommentVote,
    ) {
        if (postId <= 0L || userId <= 0L || comment.id <= 0L ||
            !comment.isVisible || comment.authorDelete || target == ArticleCommentVote.None
        ) return
        val key = CommentVoteKey(postId, userId, comment.id)
        if (requestJobs[key]?.isActive == true) return
        val current = mutableStates.value[key] ?: CommentVoteUiState(
            vote = comment.action,
            likeNum = comment.likeNum.coerceAtLeast(0),
            hateNum = comment.hateNum.coerceAtLeast(0),
        )
        if (current.isCoolingDown || current.isPending) return
        val action = when (target) {
            ArticleCommentVote.Like -> when (current.vote) {
                ArticleCommentVote.None -> ArticleCommentVoteAction.Like
                ArticleCommentVote.Like -> ArticleCommentVoteAction.CancelLike
                ArticleCommentVote.Dislike -> return
            }
            ArticleCommentVote.Dislike -> when (current.vote) {
                ArticleCommentVote.None -> ArticleCommentVoteAction.Dislike
                ArticleCommentVote.Dislike -> ArticleCommentVoteAction.CancelDislike
                ArticleCommentVote.Like -> return
            }
            ArticleCommentVote.None -> return
        }
        mutableStates.update { it + (key to current.copy(isPending = true, error = null)) }
        requestJobs[key] = coordinatorScope.launch {
            val result = repository.setCommentVote(postId, comment.id, action, userId)
            if ((authRepository.state.value as? AuthState.Authenticated)?.userId != userId) {
                mutableStates.update { it - key }
                requestJobs.remove(key)
                return@launch
            }
            when (result) {
                is DataResult.Success -> {
                    val (vote, likeDelta, hateDelta) = when (action) {
                        ArticleCommentVoteAction.Like -> Triple(ArticleCommentVote.Like, 1, 0)
                        ArticleCommentVoteAction.CancelLike -> Triple(ArticleCommentVote.None, -1, 0)
                        ArticleCommentVoteAction.Dislike -> Triple(ArticleCommentVote.Dislike, 0, 1)
                        ArticleCommentVoteAction.CancelDislike -> Triple(ArticleCommentVote.None, 0, -1)
                    }
                    val deadline = nowMillis() + COOLDOWN_MILLIS
                    mutableStates.update { states ->
                        val before = states[key] ?: current
                        states + (key to before.copy(
                            vote = vote,
                            likeNum = (before.likeNum + likeDelta).coerceAtLeast(0),
                            hateNum = (before.hateNum + hateDelta).coerceAtLeast(0),
                            isPending = false,
                            cooldownUntilMillis = deadline,
                            error = null,
                        ))
                    }
                    cooldownJobs.remove(key)?.cancel()
                    cooldownJobs[key] = coordinatorScope.launch {
                        delay(COOLDOWN_MILLIS)
                        mutableStates.update { states ->
                            val state = states[key]
                            if (state?.cooldownUntilMillis == deadline) states + (key to state.copy(cooldownUntilMillis = 0L))
                            else states
                        }
                        cooldownJobs.remove(key)
                    }
                }
                is DataResult.Failure -> {
                    val id = ++feedbackId
                    mutableStates.update { states ->
                        val before = states[key] ?: current
                        states + (key to before.copy(isPending = false, feedbackId = id, error = result.error))
                    }
                }
            }
            requestJobs.remove(key)
        }
    }

    private companion object {
        const val COOLDOWN_MILLIS = 2_000L
    }
}

@OptIn(ExperimentalTime::class)
private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
