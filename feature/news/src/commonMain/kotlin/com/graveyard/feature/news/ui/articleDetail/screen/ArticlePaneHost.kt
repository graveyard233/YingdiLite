package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.graveyard.feature.news.navigation.NewsLayoutMode
import com.graveyard.feature.news.navigation.NewsPresentation
import com.graveyard.feature.news.viewmodel.ArticleUiState
import com.graveyard.core.designsystem.webview.RichContentReadingState
import com.graveyard.core.model.news.NewsAuthor
import kotlinx.coroutines.launch

private const val DETAIL_PAGE = 0
private const val COMMENTS_PAGE = 1
private const val PAGE_COUNT = 2

@Composable
internal fun ArticlePaneHost(
    uiState: ArticleUiState,
    newsAuthor: NewsAuthor,
    presentation: NewsPresentation,
    onBack: () -> Unit,
    onMoreClicked: () -> Unit,
    readingState: RichContentReadingState,
    onRetry: () -> Unit,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    replyNum: String = "",
    likeNum: String = "",
    onWriteCommentClick: () -> Unit = {},
    onFireClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
) {
    val pageState = rememberSaveableStateHolder()
    var singlePaneFocus by rememberSaveable { mutableStateOf(DETAIL_PAGE) }
    val pagerState = rememberPagerState(
        initialPage = singlePaneFocus,
        pageCount = { PAGE_COUNT },
    )
    val coroutineScope = rememberCoroutineScope()
    // Latched on the first progress frame of a predictive gesture; consumed on completion/cancel.
    var predictiveBackInProgress by remember { mutableStateOf(false) }
    val backHandlerState = rememberNavigationEventState(
        currentInfo = ArticleNavigationEventInfo,
        backInfo = listOf(ArticleNavigationEventInfo),
        forwardInfo = emptyList(),
    )
    LaunchedEffect(presentation.panesSideBySide) {
        if (presentation.panesSideBySide) {
            // Abort a partially dragged page without persisting its pixel offset.
            pagerState.requestScrollToPage(singlePaneFocus)
            return@LaunchedEffect
        }
        launch {
            snapshotFlow { pagerState.settledPage }.collect { singlePaneFocus = it }
        }
        // Follow the system back gesture: the comments page slides with the finger, revealing
        // detail. Direction is fixed in scroll-logical space (back = previous page) regardless of
        // the swipe edge: the pager mirrors physical motion for RTL itself, and the last page has
        // no forward room for an edge-mirrored drag. Switch the anchor page at the midpoint because
        // pageOffsetFraction only covers -0.5..0.5 around its anchor.
        snapshotFlow {
            (backHandlerState.transitionState as? NavigationEventTransitionState.InProgress)
                ?.latestEvent
        }.collect { event ->
            if (event == null) return@collect
            predictiveBackInProgress = true
            val progress = event.progress.coerceIn(0f, 1f)
            if (progress <= 0.5f) {
                pagerState.requestScrollToPage(COMMENTS_PAGE, pageOffsetFraction = -progress)
            } else {
                pagerState.requestScrollToPage(DETAIL_PAGE, pageOffsetFraction = 1f - progress)
            }
        }
    }
    // Keep handling back throughout a page transition, and never drop the handler mid-gesture
    // when the dragged pager position crosses the page midpoint.
    val commentsAwaitingBack = !presentation.panesSideBySide && (
        predictiveBackInProgress
            || singlePaneFocus == COMMENTS_PAGE
            || pagerState.targetPage == COMMENTS_PAGE
        )

    fun handleBack() {
        if (!presentation.interactive) return
        if (commentsAwaitingBack) {
            coroutineScope.launch {
                if (predictiveBackInProgress) {
                    // The gesture already dragged the page into place; settle without animation.
                    pagerState.scrollToPage(DETAIL_PAGE)
                } else {
                    // Hardware/button back has no progress stream: play the return animation.
                    pagerState.animateScrollToPage(DETAIL_PAGE)
                }
                singlePaneFocus = DETAIL_PAGE
                predictiveBackInProgress = false
            }
        } else {
            onBack()
        }
    }

    NavigationBackHandler(
        state = backHandlerState,
        isBackEnabled = presentation.interactive && commentsAwaitingBack,
        onBackCancelled = {
            predictiveBackInProgress = false
            coroutineScope.launch { pagerState.animateScrollToPage(COMMENTS_PAGE) }
        },
        onBackCompleted = ::handleBack,
    )

    if (presentation.panesSideBySide) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                Box {
                    pageState.SaveableStateProvider(DETAIL_PAGE) {
                        ArticleDetailPage(
                            uiState = uiState,
                            newsAuthor = newsAuthor,
                            onBack = ::handleBack,
                            onMoreClicked = onMoreClicked,
                            readingState = readingState,
                            onRetry = onRetry,
                            onImageClick = onImageClick,
                            replyNum = replyNum,
                            likeNum = likeNum,
                            onWriteCommentClick = onWriteCommentClick,
                            onFireClick = onFireClick,
                            onFavoriteClick = onFavoriteClick,
                            contentVisible = presentation.interactive,
                            closeSelection = presentation.layoutMode == NewsLayoutMode.ReadingTriple,
                        )
                    }
                }
                Box {
                    pageState.SaveableStateProvider(COMMENTS_PAGE) {
                        ArticleCommentsPage(uiState, ::handleBack, showBack = false)
                    }
                }
            },
        ) { measurables, constraints ->
            val detailWidth = (presentation.detailWidth * density).toInt().coerceIn(0, constraints.maxWidth)
            val commentsX = (presentation.commentsStart * density).toInt().coerceIn(detailWidth, constraints.maxWidth)
            val detail = measurables[0].measure(Constraints.fixed(detailWidth, constraints.maxHeight))
            val comments = measurables[1].measure(Constraints.fixed(constraints.maxWidth - commentsX, constraints.maxHeight))
            layout(constraints.maxWidth, constraints.maxHeight) {
                detail.place(0, 0)
                comments.place(commentsX, 0)
            }
        }
    } else HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = presentation.interactive && !predictiveBackInProgress,
        key = { page -> page },
    ) { page ->
        pageState.SaveableStateProvider(page) {
            when (page) {
                DETAIL_PAGE -> ArticleDetailPage(
                    uiState = uiState,
                    newsAuthor = newsAuthor,
                    onBack = ::handleBack,
                    onMoreClicked = onMoreClicked,
                    readingState = readingState,
                    contentVisible = presentation.interactive && pagerState.settledPage == DETAIL_PAGE,
                    onRetry = onRetry,
                    onImageClick = onImageClick,
                    replyNum = replyNum,
                    likeNum = likeNum,
                    onWriteCommentClick = onWriteCommentClick,
                    onFireClick = onFireClick,
                    onFavoriteClick = onFavoriteClick,
                    onCommentsClick = {
                        if (presentation.interactive) {
                            coroutineScope.launch { pagerState.animateScrollToPage(COMMENTS_PAGE) }
                        }
                    },
                )

                COMMENTS_PAGE -> ArticleCommentsPage(
                    uiState = uiState,
                    onBack = ::handleBack,
                )
            }
        }
    }
}

private object ArticleNavigationEventInfo : NavigationEventInfo()
