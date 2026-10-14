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
import com.graveyard.feature.news.viewmodel.BannerUiState
import com.graveyard.feature.news.viewmodel.NewsViewModel
import org.jetbrains.compose.resources.getString
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.news_refresh_failure
import yingdilite.feature.news.generated.resources.news_refresh_section_news
import yingdilite.feature.news.generated.resources.news_refresh_section_recommendations
import yingdilite.feature.news.generated.resources.news_refresh_section_separator
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
internal fun NewsCategoryPage(
    tagId: Int,
    viewModel: NewsViewModel,
    pagingItemsByTag: SnapshotStateMap<Int, LazyPagingItems<NewsArticle>>,
    isRefreshing: Boolean,
    stateHolder: SaveableStateHolder,
    isCurrentPage: (Int) -> Boolean,
    onPageVisited: (Int) -> Unit,
    onShowRefreshFailure: (Int, String) -> Unit,
    onArticleClick: (String, String) -> Unit,
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
        val currentPage by androidx.compose.runtime.rememberUpdatedState(isCurrentPage)
        val showFailure by androidx.compose.runtime.rememberUpdatedState(onShowRefreshFailure)
        LaunchedEffect(viewModel, tagId, listState) {
            viewModel.refreshResults.collect { result ->
                if (result.tagId != tagId || !currentPage(tagId)) return@collect
                if (result.newsSucceeded) listState.scrollToItem(0)
                val failures = buildList {
                    if (result.newsFailed) add(getString(Res.string.news_refresh_section_news))
                    if (result.bannersFailed) add(getString(Res.string.news_refresh_section_recommendations))
                }
                if (failures.isNotEmpty() && currentPage(tagId)) {
                    showFailure(tagId, getString(Res.string.news_refresh_failure,
                        failures.joinToString(getString(Res.string.news_refresh_section_separator))))
                }
            }
        }
        NewsPage(
            bannerState = bannerState,
            newsItems = newsItems,
            listState = listState,
            isRefreshing = isRefreshing,
            onRetryBanners = {
                if (!isRefreshing &&
                    viewModel.uiState.value.bannersByTag[tagId]?.isLoading != true
                ) {
                    viewModel.reloadBanners(tagId)
                }
            },
            onRefresh = {
                if (isCurrentPage(tagId) &&
                    newsItems != null &&
                    !isRefreshing &&
                    newsItems.loadState.refresh !is LoadState.Loading &&
                    !bannerState.isLoading
                ) {
                    viewModel.refresh(tagId, newsItems.itemCount > 0, bannerState.items.isNotEmpty()) {
                        newsItems.refresh()
                    }
                }
            },
            onArticleClick = onArticleClick,
            selectedArticleId = selectedArticleId,
        )
    }
}
