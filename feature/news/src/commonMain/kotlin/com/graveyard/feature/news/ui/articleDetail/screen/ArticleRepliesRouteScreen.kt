package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.graveyard.feature.news.navigation.LocalNewsPresentation
import com.graveyard.feature.news.viewmodel.ArticleRepliesViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ArticleRepliesRouteScreen(
    commentId: Long,
    onBack: () -> Unit,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    viewModel: ArticleRepliesViewModel = koinViewModel { parametersOf(commentId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val interactive = LocalNewsPresentation.current.interactive
    val listState = rememberLazyListState()
    var lastOrder by rememberSaveable { mutableStateOf(state.order.name) }
    LaunchedEffect(state.order) {
        if (lastOrder != state.order.name) {
            listState.scrollToItem(0)
            lastOrder = state.order.name
        }
    }
    val replies = viewModel.replies.collectAsLazyPagingItems()
    ArticleRepliesPage(
        commentId = commentId,
        state = state,
        replies = replies,
        listState = listState,
        onBack = { if (interactive) onBack() },
        onLoad = viewModel::loadReplies,
        onOrderSelected = { if (interactive) viewModel.selectOrder(it) },
        onImageClick = { urls, index -> if (interactive) onImageClick(urls, index) },
        onLikeClicked = viewModel::onLikeClicked,
        onDislikeClicked = viewModel::onDislikeClicked,
        onMoreClicked = viewModel::onMoreClicked,
    )
}
