package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.graveyard.core.model.news.NewsAuthor
import com.graveyard.feature.news.navigation.LocalNewsPresentation
import com.graveyard.feature.news.navigation.NewsPresentation
import com.graveyard.feature.news.viewmodel.ArticleViewModel
import com.graveyard.core.designsystem.webview.RichContentReadingState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ArticleRouteScreen(
    articleId: String,
    articleTitle: String,
    newsAuthor: NewsAuthor,
    onBack: () -> Unit,
    presentation: NewsPresentation = LocalNewsPresentation.current,
    viewModel: ArticleViewModel = koinViewModel { parametersOf(articleId, articleTitle) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val readingState = remember(articleId) { RichContentReadingState() }
    ArticlePaneHost(
        uiState = uiState,
        newsAuthor = newsAuthor,
        presentation = presentation,
        onBack = onBack,
        onMoreClicked = viewModel::onMoreClicked,
        readingState = readingState,
        onRetry = viewModel::loadArticle,
    )
}
