package com.graveyard.feature.news.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.graveyard.core.model.news.NewsArticle
import com.graveyard.feature.news.model.DefaultNewsCategories
import com.graveyard.feature.news.model.NewsCategory
import com.graveyard.feature.news.model.validateNewsCategories
import com.graveyard.feature.news.viewmodel.BannerUiState
import com.graveyard.feature.news.viewmodel.NewsViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsRouteScreen(
    onShowMessage: suspend (String) -> Unit,
    categories: List<NewsCategory> = DefaultNewsCategories,
    viewModel: NewsViewModel = koinViewModel(),
) {
    validateNewsCategories(categories)
    val categoryIds = categories.map { it.id }
    val uiState by viewModel.uiState.collectAsState()
    val activeTagId = uiState.selectedTagId?.takeIf { it in categoryIds } ?: categoryIds.first()
    val currentTagId by rememberUpdatedState(activeTagId)
    val currentCategoryIds by rememberUpdatedState(categoryIds)
    val showMessage by rememberUpdatedState(onShowMessage)
    val stateHolder = rememberSaveableStateHolder()
    val pagerState = rememberPagerState(
        initialPage = categoryIds.indexOf(activeTagId),
        pageCount = { categories.size },
    )
    val scope = rememberCoroutineScope()
    val refreshJobs = remember { mutableStateMapOf<Int, Job>() }
    var messageJob by remember { mutableStateOf<Job?>(null) }
    var visitedTagIds by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var previousCategoryIds by rememberSaveable { mutableStateOf(categoryIds) }
    var pagerCategoryIds by remember { mutableStateOf(emptyList<Int>()) }
    var isSynchronizingPager by remember { mutableStateOf(true) }
    var requestedTagId by remember { mutableStateOf<Int?>(null) }
    var pagerNavigationRequest by remember { mutableStateOf(0) }

    val isCurrentPage: (Int) -> Boolean = { tagId ->
        currentTagId == tagId &&
            !isSynchronizingPager &&
            !pagerState.isScrollInProgress &&
            currentCategoryIds.getOrNull(pagerState.settledPage) == tagId
    }

    LaunchedEffect(categoryIds) {
        val removedIds = previousCategoryIds.filter { it !in categoryIds }
        removedIds.forEach { tagId ->
            refreshJobs.remove(tagId)?.cancel()
            stateHolder.removeState(tagId)
        }
        visitedTagIds = visitedTagIds.filter { it in categoryIds }
        previousCategoryIds = categoryIds
        viewModel.updateCategories(categoryIds)
    }
    LaunchedEffect(activeTagId) {
        messageJob?.cancel()
        visitedTagIds = (visitedTagIds + activeTagId).distinct()
    }
    LaunchedEffect(categoryIds, activeTagId, pagerNavigationRequest) {
        val request = pagerNavigationRequest
        isSynchronizingPager = true
        try {
            val page = categoryIds.indexOf(activeTagId)
            if (pagerCategoryIds != categoryIds) {
                pagerCategoryIds = categoryIds
                pagerState.scrollToPage(page)
            } else if (requestedTagId != null || pagerState.settledPage != page) {
                pagerState.animateScrollToPage(page)
            }
        } finally {
            if (pagerNavigationRequest == request) {
                requestedTagId = null
                isSynchronizingPager = false
            }
        }
    }
    LaunchedEffect(pagerState, viewModel) {
        snapshotFlow {
            // Ignore intermediate pages while a tab selection is animating.
            if (
                isSynchronizingPager || requestedTagId != null ||
                pagerState.isScrollInProgress || pagerCategoryIds != currentCategoryIds
            ) {
                null
            } else {
                currentCategoryIds.getOrNull(pagerState.settledPage)
            }
        }.filterNotNull().collect { tagId ->
            if (viewModel.uiState.value.selectedTagId != tagId) viewModel.selectTag(tagId)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            messageJob?.cancel()
            refreshJobs.values.toList().forEach { it.cancel() }
            refreshJobs.clear()
        }
    }

    // Keep visited presenters alive so a refresh can finish after switching categories.
    val pagingItemsByTag = mutableMapOf<Int, LazyPagingItems<NewsArticle>>()
    val visibleTagIds = pagerState.layoutInfo.visiblePagesInfo.mapNotNull { page ->
        categoryIds.getOrNull(page.index)
    }
    val targetTagId = categoryIds.getOrNull(pagerState.targetPage)
    val collectedIds = (visitedTagIds + activeTagId + visibleTagIds + listOfNotNull(targetTagId))
        .distinct()
        .filter { it in categoryIds }
    for (tagId in collectedIds) {
        key(tagId) {
            val news = remember(viewModel, tagId) { viewModel.newsFor(tagId) }
            pagingItemsByTag[tagId] = news.collectAsLazyPagingItems()
        }
    }

    NewsScreen(
        categories = categories,
        pagerState = pagerState,
        onSelectCategory = { category ->
            messageJob?.cancel()
            isSynchronizingPager = true
            requestedTagId = category.id
            pagerNavigationRequest++
            visitedTagIds = (visitedTagIds + category.id).distinct()
            viewModel.selectTag(category.id)
        },
        pageContent = { category ->
            val tagId = category.id
            LaunchedEffect(tagId) {
                visitedTagIds = (visitedTagIds + tagId).distinct()
                if (tagId !in viewModel.uiState.value.bannersByTag) {
                    viewModel.reloadBanners(tagId)
                }
            }
            stateHolder.SaveableStateProvider(tagId) {
                val listState = rememberLazyListState()
                val newsItems = pagingItemsByTag[tagId]
                val bannerState = uiState.bannersByTag[tagId] ?: BannerUiState(isLoading = true)
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

                                    val banners = viewModel.uiState.value.bannersByTag[tagId]
                                    val failures = buildList {
                                        if (hadNews && newsResult.error != null) add("新闻")
                                        if (hadBanners && banners?.error != null) add("推荐内容")
                                    }
                                    if (isCurrentPage(tagId) && failures.isNotEmpty()) {
                                        messageJob?.cancel()
                                        messageJob = scope.launch {
                                            if (isCurrentPage(tagId)) {
                                                showMessage(
                                                    "${failures.joinToString("、")}刷新失败，已保留原内容",
                                                )
                                            }
                                        }
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
        },
    )
}
