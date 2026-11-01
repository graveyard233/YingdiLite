package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.graveyard.core.data.repository.ArticleVote
import com.graveyard.feature.news.navigation.LocalNewsPresentation
import com.graveyard.feature.news.navigation.NewsPresentation
import com.graveyard.feature.news.viewmodel.ArticleViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ArticleRouteScreen(
    articleId: String,
    articleTitle: String,
    onBack: () -> Unit,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    onViewReplies: (commentId: Long) -> Unit,
    onCardOpen: (id: String, title: String) -> Unit = { _, _ -> },
    replyNum: String = "",
    likeNum: String = "",
    onWriteCommentClick: () -> Unit = {},
    onFireClick: () -> Unit = {},
    onHateClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    presentation: NewsPresentation = LocalNewsPresentation.current,
    viewModel: ArticleViewModel = koinViewModel { parametersOf(articleId, articleTitle) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    val parsedContent = uiState.content
    val resolvedReplyNum = parsedContent?.replyNum.orEmpty().ifEmpty { replyNum }
    val resolvedLikeNum = parsedContent?.likeNum.orEmpty().ifEmpty { likeNum }
    // Guests and pages whose SSR did not recognise the session have no viewer: show inactive states.
    val viewer = parsedContent?.viewer
    ArticlePaneHost(
        uiState = uiState,
        commentsState = commentsState,
        replyNum = resolvedReplyNum,
        likeNum = resolvedLikeNum,
        vote = viewer?.vote ?: ArticleVote.None,
        collected = viewer?.collected == true,
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
        onCommentLikeClicked = viewModel::onCommentLikeClicked,
        onCommentDislikeClicked = viewModel::onCommentDislikeClicked,
        onCommentMoreClicked = viewModel::onCommentMoreClicked,
        onWriteCommentClick = onWriteCommentClick,
        onFireClick = onFireClick,
        onHateClick = onHateClick,
        onFavoriteClick = onFavoriteClick,
    )
}
