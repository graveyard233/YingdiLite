package com.graveyard.feature.news.ui.newsList.screen

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.graveyard.core.model.news.NewsArticle
import com.graveyard.core.model.news.NewsAuthor
import com.graveyard.feature.news.viewmodel.BannerUiState
import com.graveyard.feature.news.viewmodel.NewsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
internal fun NewsCategoryPage(
    tagId: Int,
    viewModel: NewsViewModel,
    pagingItemsByTag: SnapshotStateMap<Int, LazyPagingItems<NewsArticle>>,
    stateHolder: SaveableStateHolder,
    isCurrentPage: (Int) -> Boolean,
    onPageVisited: (Int) -> Unit,
    onArticleClick: (String, String, NewsAuthor, String, String) -> Unit,
    selectedArticleId: String? = null,
) {
    val banners = remember(viewModel, tagId) {
        viewModel.uiState.map { state ->
            state.bannersByTag[tagId] ?: BannerUiState(isLoading = true)
        }.distinctUntilChanged()
    }
    val bannerState by banners.collectAsState(
        initial = viewModel.uiState.value.bannersByTag[tagId] ?: BannerUiState(isLoading = true),
    )
    LaunchedEffect(viewModel, tagId) {
        onPageVisited(tagId)
        if (tagId !in viewModel.uiState.value.bannersByTag) {
            viewModel.reloadBanners(tagId)
        }
    }
    stateHolder.SaveableStateProvider(tagId) {
        val listState = rememberLazyListState()
        val newsItems = pagingItemsByTag[tagId]
        NewsPage(
            bannerState = bannerState,
            newsItems = newsItems,
            listState = listState,
            onRetryBanners = {
                if (viewModel.uiState.value.bannersByTag[tagId]?.isLoading != true) {
                    viewModel.reloadBanners(tagId)
                }
            },
            onRefresh = {
                if (isCurrentPage(tagId) &&
                    newsItems != null &&
                    newsItems.loadState.refresh !is LoadState.Loading
                ) {
                    viewModel.reloadBanners(tagId)
                    newsItems.refresh()
                }
            },
            onArticleClick = onArticleClick,
            selectedArticleId = selectedArticleId,
        )
    }
}
