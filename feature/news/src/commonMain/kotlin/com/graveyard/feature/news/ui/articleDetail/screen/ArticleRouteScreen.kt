package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.graveyard.core.model.news.NewsAuthor
import com.graveyard.feature.news.navigation.LocalNewsPresentation
import com.graveyard.feature.news.navigation.NewsPresentation
import com.graveyard.feature.news.viewmodel.ArticleViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ArticleRouteScreen(
    articleId: String,
    articleTitle: String,
    newsAuthor: NewsAuthor,
    onBack: () -> Unit,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    replyNum: String = "",
    likeNum: String = "",
    onWriteCommentClick: () -> Unit = {},
    onFireClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    presentation: NewsPresentation = LocalNewsPresentation.current,
    viewModel: ArticleViewModel = koinViewModel { parametersOf(articleId, articleTitle) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ArticlePaneHost(
        uiState = uiState,
        newsAuthor = newsAuthor,
        replyNum = replyNum,
        likeNum = likeNum,
        presentation = presentation,
        onBack = onBack,
        onMoreClicked = viewModel::onMoreClicked,
        readingState = viewModel.readingState,
        onRetry = viewModel::loadArticle,
        onImageClick = onImageClick,
        onWriteCommentClick = onWriteCommentClick,
        onFireClick = onFireClick,
        onFavoriteClick = onFavoriteClick,
    )
}
