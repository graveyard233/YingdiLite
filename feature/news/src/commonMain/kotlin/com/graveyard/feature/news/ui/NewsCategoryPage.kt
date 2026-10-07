package com.graveyard.feature.news.ui

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
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.getString
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.news_refresh_failure
import yingdilite.feature.news.generated.resources.news_refresh_section_news
import yingdilite.feature.news.generated.resources.news_refresh_section_recommendations
import yingdilite.feature.news.generated.resources.news_refresh_section_separator
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
internal fun NewsCategoryPage(
    tagId: Int,
    viewModel: NewsViewModel,
    pagingItemsByTag: SnapshotStateMap<Int, LazyPagingItems<NewsArticle>>,
    refreshJobs: SnapshotStateMap<Int, Job>,
    stateHolder: SaveableStateHolder,
    scope: CoroutineScope,
    isCurrentPage: (Int) -> Boolean,
    onPageVisited: (Int) -> Unit,
    onShowRefreshFailure: (Int, String) -> Unit,
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
            isRefreshing = tagId in refreshJobs,
            onRetryBanners = {
                if (tagId !in refreshJobs &&
                    viewModel.uiState.value.bannersByTag[tagId]?.isLoading != true
                ) {
                    viewModel.reloadBanners(tagId)
                }
            },
            onRefresh = {
                if (isCurrentPage(tagId) &&
                    newsItems != null &&
                    tagId !in refreshJobs &&
                    newsItems.loadState.refresh !is LoadState.Loading &&
                    !bannerState.isLoading
                ) {
                    val refreshState = viewModel.pagingRefreshStateFor(tagId)
                    val previousRequest = refreshState.value.request
                    val hadNews = newsItems.itemCount > 0
                    val hadBanners = bannerState.items.isNotEmpty()
                    val job = scope.launch(start = CoroutineStart.LAZY) {
                        try {
                            val bannerJob = viewModel.reloadBanners(tagId)
                            newsItems.refresh()
                            val newsResult = refreshState.first {
                                it.request !== previousRequest && !it.isLoading
                            }
                            bannerJob.join()
                            if (newsResult.isCancelled || bannerJob.isCancelled) return@launch
                            if (isCurrentPage(tagId) && newsResult.error == null) {
                                listState.scrollToItem(0)
                            }

                            val refreshedBanners = viewModel.uiState.value.bannersByTag[tagId]
                            val failures = buildList {
                                if (hadNews && newsResult.error != null) {
                                    add(getString(Res.string.news_refresh_section_news))
                                }
                                if (hadBanners && refreshedBanners?.error != null) {
                                    add(getString(Res.string.news_refresh_section_recommendations))
                                }
                            }
                            if (isCurrentPage(tagId) && failures.isNotEmpty()) {
                                onShowRefreshFailure(
                                    tagId,
                                    getString(
                                        Res.string.news_refresh_failure,
                                        failures.joinToString(
                                            getString(Res.string.news_refresh_section_separator),
                                        ),
                                    ),
                                )
                            }
                        } finally {
                            if (refreshJobs[tagId] === currentCoroutineContext()[Job]) {
                                refreshJobs.remove(tagId)
                            }
                        }
                    }
                    refreshJobs[tagId] = job
                    job.start()
                }
            },
        )
    }
}
