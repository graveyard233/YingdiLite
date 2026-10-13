package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.graveyard.feature.news.viewmodel.ArticleViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val DETAIL_PAGE = 0
private const val COMMENTS_PAGE = 1
private const val PAGE_COUNT = 2

@Composable
fun ArticleRouteScreen(
    articleId: String,
    articleTitle: String,
    onBack: () -> Unit,
    viewModel: ArticleViewModel = koinViewModel {
        parametersOf(articleId, articleTitle)
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(
        initialPage = DETAIL_PAGE,
        pageCount = { PAGE_COUNT },
    )
    val coroutineScope = rememberCoroutineScope()
    // Keep handling back throughout a page transition, including a return animation.
    val hasCommentsVisible = pagerState.currentPage == COMMENTS_PAGE ||
        pagerState.targetPage == COMMENTS_PAGE ||
        pagerState.settledPage == COMMENTS_PAGE
    val backHandlerState = rememberNavigationEventState(
        currentInfo = ArticleNavigationEventInfo,
        backInfo = listOf(ArticleNavigationEventInfo),
        forwardInfo = emptyList(),
    )

    fun handleBack() {
        if (hasCommentsVisible) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(DETAIL_PAGE)
            }
        } else {
            onBack()
        }
    }

    NavigationBackHandler(
        state = backHandlerState,
        isBackEnabled = hasCommentsVisible,
        onBackCancelled = {},
        onBackCompleted = ::handleBack,
    )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true,
        key = { page -> page },
    ) { page ->
        when (page) {
            DETAIL_PAGE -> ArticleDetailPage(
                uiState = uiState,
                onBack = ::handleBack,
                onMoreClicked = viewModel::onMoreClicked,
            )

            COMMENTS_PAGE -> ArticleCommentsPage(
                uiState = uiState,
                onBack = ::handleBack,
            )
        }
    }
}

private object ArticleNavigationEventInfo : NavigationEventInfo()
