package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.material3.SnackbarHostState
import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.data.result.DataError
import com.graveyard.core.designsystem.navigation.auth.AuthContinuation
import com.graveyard.core.designsystem.navigation.auth.AuthResult
import com.graveyard.core.model.news.ArticleComment
import com.graveyard.core.model.news.ArticleCommentVote
import com.graveyard.feature.news.navigation.ArticleAuthAction
import com.graveyard.feature.news.navigation.LocalNewsPresentation
import com.graveyard.feature.news.viewmodel.ArticleRepliesViewModel
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_auth_hate
import yingdilite.feature.news.generated.resources.article_auth_like
import yingdilite.feature.news.generated.resources.article_interaction_failed

@Composable
fun ArticleRepliesRouteScreen(
    articleId: String,
    commentId: Long,
    onBack: () -> Unit,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    authState: AuthState,
    authResult: AuthResult?,
    onTakeAuthResult: (AuthResult) -> AuthResult?,
    onRequireLogin: (AuthContinuation, String) -> Unit,
    viewModel: ArticleRepliesViewModel = koinViewModel { parametersOf(articleId, commentId) },
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val interactive = LocalNewsPresentation.current.interactive
    val userId = (authState as? AuthState.Authenticated)?.userId
    val voteStatesFlow = remember(viewModel, userId) { viewModel.commentVoteStates(userId) }
    val voteStates by voteStatesFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
    var lastFeedbackId by rememberSaveable { mutableStateOf(0L) }
    val likeMessage = stringResource(Res.string.article_auth_like)
    val dislikeMessage = stringResource(Res.string.article_auth_hate)
    fun submitVote(comment: ArticleComment, target: ArticleCommentVote) {
        if (!interactive || authState == AuthState.Restoring || authResult != null) return
        viewModel.cacheCommentSnapshot(comment)
        val postId = articleId.toLongOrNull() ?: return
        when (authState) {
            AuthState.Guest -> onRequireLogin(ArticleAuthAction.SetCommentVote(postId, comment.id, target),
                if (target == ArticleCommentVote.Dislike) dislikeMessage else likeMessage)
            is AuthState.Authenticated -> viewModel.vote(comment, target, authState.userId)
            AuthState.Restoring -> Unit
        }
    }
    val listState = rememberLazyListState()
    var lastOrder by rememberSaveable { mutableStateOf(state.order.name) }
    LaunchedEffect(state.order) {
        if (lastOrder != state.order.name) {
            listState.scrollToItem(0)
            lastOrder = state.order.name
        }
    }
    val replies = viewModel.replies.collectAsLazyPagingItems()
    LaunchedEffect(authResult?.request?.requestId, userId, interactive) {
        if (interactive && authResult != null && userId == authResult.userId &&
            viewModel.canAcceptInteraction(authResult.userId)) {
            val claimed = onTakeAuthResult(authResult) ?: return@LaunchedEffect
            (claimed.request.continuation as? ArticleAuthAction)?.let { viewModel.performInteraction(it, claimed.userId) }
        }
    }
    LaunchedEffect(voteStates, interactive) {
        if (!interactive) return@LaunchedEffect
        voteStates.values.firstOrNull { it.feedbackId > lastFeedbackId }?.let { feedback ->
            lastFeedbackId = feedback.feedbackId
            snackbarHostState.showSnackbar((feedback.error as? DataError.Business)?.retMsg?.takeIf { it.isNotBlank() }
                ?: getString(Res.string.article_interaction_failed))
        }
    }
    ArticleRepliesPage(
        commentId = commentId,
        state = state,
        replies = replies,
        listState = listState,
        onBack = { if (interactive) onBack() },
        onLoad = viewModel::loadReplies,
        onOrderSelected = { if (interactive) viewModel.selectOrder(it) },
        onImageClick = { urls, index -> if (interactive) onImageClick(urls, index) },
        onLikeClicked = { comment, _ -> submitVote(comment, ArticleCommentVote.Like) },
        onDislikeClicked = { comment, _ -> submitVote(comment, ArticleCommentVote.Dislike) },
        onMoreClicked = viewModel::onMoreClicked,
        voteStates = voteStates,
        snackbarHostState = snackbarHostState,
    )
}
