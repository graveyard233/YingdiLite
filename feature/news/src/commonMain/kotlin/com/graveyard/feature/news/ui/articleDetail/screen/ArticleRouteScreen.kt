package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.SnackbarHostState
import com.graveyard.core.data.repository.ArticleVote
import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.data.result.DataError
import com.graveyard.core.model.news.ArticleComment
import com.graveyard.core.model.news.ArticleCommentVote
import com.graveyard.core.designsystem.navigation.auth.AuthContinuation
import com.graveyard.core.designsystem.navigation.auth.AuthResult
import com.graveyard.feature.news.navigation.ArticleAuthAction
import com.graveyard.feature.news.navigation.LocalNewsPresentation
import com.graveyard.feature.news.navigation.NewsPresentation
import com.graveyard.feature.news.viewmodel.ArticleViewModel
import com.graveyard.feature.news.viewmodel.ArticleInteractionMessage
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_auth_like
import yingdilite.feature.news.generated.resources.article_auth_hate
import yingdilite.feature.news.generated.resources.article_auth_collect
import yingdilite.feature.news.generated.resources.article_interaction_sync_failed
import yingdilite.feature.news.generated.resources.article_interaction_failed
import yingdilite.feature.news.generated.resources.article_interaction_saved_sync_failed
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import yingdilite.feature.news.generated.resources.article_already_hated
import yingdilite.feature.news.generated.resources.article_already_liked

@Composable
fun ArticleRouteScreen(
    articleId: String,
    articleTitle: String,
    onBack: () -> Unit,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    onViewReplies: (commentId: Long) -> Unit,
    authState: AuthState,
    authResult: AuthResult?,
    onTakeAuthResult: (AuthResult) -> AuthResult?,
    onRequireLogin: (AuthContinuation, String) -> Unit,
    onCardOpen: (id: String, title: String) -> Unit = { _, _ -> },
    replyNum: String = "",
    likeNum: String = "",
    onWriteCommentClick: () -> Unit = {},
    presentation: NewsPresentation = LocalNewsPresentation.current,
    viewModel: ArticleViewModel = koinViewModel { parametersOf(articleId, articleTitle) },
) {
    val articleSnackbarHostState = remember { SnackbarHostState() }
    val commentsSnackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voteCooldownProgress = remember { Animatable(1f) }
    LaunchedEffect(uiState.isVoteCoolingDown) {
        if (uiState.isVoteCoolingDown) {
            voteCooldownProgress.snapTo(0f)
            voteCooldownProgress.animateTo(1f, tween(durationMillis = 5_000))
        } else {
            voteCooldownProgress.snapTo(1f)
        }
    }
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    val userId = (authState as? AuthState.Authenticated)?.userId
    val commentVoteStatesFlow = remember(viewModel, userId) { viewModel.commentVoteStates(userId) }
    val commentVoteStates by commentVoteStatesFlow
        .collectAsStateWithLifecycle(initialValue = emptyMap())
    var lastCommentVoteFeedbackId by remember { mutableStateOf(0L) }
    val parsedContent = uiState.content
    val interaction = uiState.interaction
    val viewer = interaction.viewer.takeIf { userId != null && interaction.userId == userId }
    val coroutineScope = rememberCoroutineScope()
    val resolvedReplyNum = parsedContent?.replyNum.orEmpty().ifEmpty { replyNum }
    val resolvedLikeNum = interaction.likeNum.takeIf { userId != null && interaction.userId == userId }
        .orEmpty().ifEmpty { parsedContent?.likeNum.orEmpty().ifEmpty { likeNum } }
    val actionsEnabled = presentation.interactive && authState != AuthState.Restoring &&
        !uiState.isLoading && parsedContent != null && !interaction.isBusy && authResult == null
    val voteEnabled = actionsEnabled && !uiState.isVoteCoolingDown
    val likeMessage = stringResource(Res.string.article_auth_like)
    val hateMessage = stringResource(Res.string.article_auth_hate)
    val collectMessage = stringResource(Res.string.article_auth_collect)
    val alreadyLikedMessage = stringResource(Res.string.article_already_liked)
    val alreadyHatedMessage = stringResource(Res.string.article_already_hated)
    fun loginMessage(action: ArticleAuthAction) = when (action) {
        is ArticleAuthAction.SetVote -> if (action.vote == ArticleVote.Hate) hateMessage else likeMessage
        is ArticleAuthAction.SetCollected -> collectMessage
        is ArticleAuthAction.SetCommentVote -> if (action.vote == ArticleCommentVote.Dislike) hateMessage else likeMessage
    }
    fun submit(action: ArticleAuthAction) {
        if (!actionsEnabled) return
        when (authState) {
            AuthState.Guest -> onRequireLogin(action, loginMessage(action))
            is AuthState.Authenticated -> viewModel.performInteraction(action, authState.userId)
            AuthState.Restoring -> Unit
        }
    }
    fun submitVote(vote: ArticleVote, snackbarHostState: SnackbarHostState) {
        if (!voteEnabled) return
        if (viewer?.vote == vote) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    if (vote == ArticleVote.Like) alreadyLikedMessage else alreadyHatedMessage,
                )
            }
            return
        }
        articleId.toLongOrNull()?.let { submit(ArticleAuthAction.SetVote(it, vote)) }
    }
    fun submitCommentVote(comment: ArticleComment, target: ArticleCommentVote) {
        if (!presentation.interactive || authState == AuthState.Restoring || authResult != null) return
        viewModel.cacheCommentSnapshot(comment)
        val postId = articleId.toLongOrNull() ?: return
        when (authState) {
            AuthState.Guest -> onRequireLogin(
                ArticleAuthAction.SetCommentVote(postId, comment.id, target),
                if (target == ArticleCommentVote.Dislike) hateMessage else likeMessage,
            )
            is AuthState.Authenticated -> viewModel.voteComment(comment.id, target, authState.userId)
            AuthState.Restoring -> Unit
        }
    }

    LaunchedEffect(userId, presentation.interactive, uiState.isLoading) {
        if (userId != null && presentation.interactive && !uiState.isLoading && authResult == null) {
            viewModel.syncInteraction()
        }
    }
    LaunchedEffect(authResult?.request?.requestId, userId, presentation.interactive, uiState.isLoading, interaction.isBusy) {
        if (presentation.interactive && authResult != null && userId == authResult.userId &&
            viewModel.canAcceptInteraction(authResult.userId)) {
            val claimed = onTakeAuthResult(authResult) ?: return@LaunchedEffect
            val action = claimed.request.continuation as? ArticleAuthAction
            if (action != null) viewModel.performInteraction(action, claimed.userId)
        }
    }
    LaunchedEffect(commentVoteStates, presentation.interactive) {
        if (!presentation.interactive) return@LaunchedEffect
        commentVoteStates.values.firstOrNull { it.feedbackId > lastCommentVoteFeedbackId }?.let { feedback ->
            lastCommentVoteFeedbackId = feedback.feedbackId
            val message = (feedback.error as? DataError.Business)?.retMsg?.takeIf { it.isNotBlank() }
                ?: getString(Res.string.article_interaction_failed)
            commentsSnackbarHostState.showSnackbar(message)
        }
    }
    val requestLogin by rememberUpdatedState({ action: ArticleAuthAction -> onRequireLogin(action, loginMessage(action)) })
    LaunchedEffect(viewModel, presentation.interactive) {
        if (!presentation.interactive) return@LaunchedEffect
        // Consuming feedback must not cancel the coroutine showing its snackbar.
        viewModel.uiState.map { it.interactionFeedback }.distinctUntilChanged().collect { pending ->
            val consumed = pending?.let { viewModel.consumeInteractionFeedback(it.id) } ?: return@collect
            consumed.requiresLogin?.let(requestLogin)
            val message = when (consumed.message) {
                ArticleInteractionMessage.SyncFailed -> getString(Res.string.article_interaction_sync_failed)
                ArticleInteractionMessage.SavedSyncFailed -> getString(Res.string.article_interaction_saved_sync_failed)
                ArticleInteractionMessage.OperationFailed -> (consumed.error as? DataError.Business)?.retMsg
                    ?.takeIf { it.isNotBlank() } ?: getString(Res.string.article_interaction_failed)
                null -> null
            }
            if (message != null) articleSnackbarHostState.showSnackbar(message)
        }
    }
    ArticlePaneHost(
        uiState = uiState,
        commentsState = commentsState,
        replyNum = resolvedReplyNum,
        likeNum = resolvedLikeNum,
        vote = viewer?.vote ?: ArticleVote.None,
        collected = viewer?.collected == true,
        interactionEnabled = actionsEnabled,
        voteEnabled = voteEnabled,
        isVoteCoolingDown = uiState.isVoteCoolingDown,
        voteCooldownProgress = { voteCooldownProgress.value },
        presentation = presentation,
        onBack = onBack,
        onMoreClicked = viewModel::onMoreClicked,
        readingState = viewModel.readingState,
        onRetry = viewModel::loadArticle,
        onImageClick = onImageClick,
        onViewReplies = onViewReplies,
        onCardOpen = onCardOpen,
        onLoadComments = viewModel::loadComments,
        onCommentOrderSelected = viewModel::selectCommentOrder,
        onCommentLikeClicked = { comment, _ -> submitCommentVote(comment, ArticleCommentVote.Like) },
        onCommentDislikeClicked = { comment, _ -> submitCommentVote(comment, ArticleCommentVote.Dislike) },
        commentVoteStates = commentVoteStates,
        articleSnackbarHostState = articleSnackbarHostState,
        commentsSnackbarHostState = commentsSnackbarHostState,
        onCommentMoreClicked = viewModel::onCommentMoreClicked,
        onWriteCommentClick = onWriteCommentClick,
        onFireClick = { snackbarHostState -> submitVote(ArticleVote.Like, snackbarHostState) },
        onHateClick = { snackbarHostState -> submitVote(ArticleVote.Hate, snackbarHostState) },
        onFavoriteClick = { articleId.toLongOrNull()?.let { submit(ArticleAuthAction.SetCollected(it, viewer?.collected != true)) } },
    )
}
