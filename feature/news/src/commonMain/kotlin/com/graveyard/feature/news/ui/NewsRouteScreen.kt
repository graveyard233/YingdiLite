package com.graveyard.feature.news.ui

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.graveyard.core.model.news.NewsArticle
import com.graveyard.feature.news.model.DefaultNewsCategories
import com.graveyard.feature.news.model.NewsCategory
import com.graveyard.feature.news.model.validateNewsCategories
import com.graveyard.feature.news.viewmodel.NewsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NewsRouteScreen(
    onShowMessage: suspend (String) -> Unit,
    categories: List<NewsCategory> = DefaultNewsCategories,
    viewModel: NewsViewModel = koinViewModel(),
) {
    val categoryIds = remember(categories) {
        validateNewsCategories(categories)
        categories.map { it.id }
    }
    val selectedTagIds = remember(viewModel) {
        viewModel.uiState.map { it.selectedTagId }.distinctUntilChanged()
    }
    val selectedTagId by selectedTagIds.collectAsState(
        initial = viewModel.uiState.value.selectedTagId,
    )
    val activeTagId = selectedTagId?.takeIf { it in categoryIds } ?: categoryIds.first()
    val currentTagId by rememberUpdatedState(activeTagId)
    val currentCategoryIds by rememberUpdatedState(categoryIds)
    val showMessage by rememberUpdatedState(onShowMessage)
    val stateHolder = rememberSaveableStateHolder()
    val pagerState = rememberPagerState(
        initialPage = categoryIds.indexOf(activeTagId),
        pageCount = { categories.size },
    )
    val scope = rememberCoroutineScope()
    val pagingItemsByTag = remember(viewModel) {
        mutableStateMapOf<Int, LazyPagingItems<NewsArticle>>()
    }
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
            pagingItemsByTag.remove(tagId)
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
    val collectionPageIndices by remember(pagerState) {
        derivedStateOf(structuralEqualityPolicy()) {
            (pagerState.layoutInfo.visiblePagesInfo.map { it.index } + pagerState.targetPage)
                .distinct()
                .sorted()
        }
    }
    val collectedIds = remember(visitedTagIds, activeTagId, collectionPageIndices, categoryIds) {
        (visitedTagIds + activeTagId + collectionPageIndices.mapNotNull { categoryIds.getOrNull(it) })
            .distinct()
            .filter { it in categoryIds }
    }
    for (tagId in collectedIds) {
        key(tagId) {
            NewsPagingCollector(tagId, viewModel, pagingItemsByTag)
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
            NewsCategoryPage(
                tagId = category.id,
                viewModel = viewModel,
                pagingItemsByTag = pagingItemsByTag,
                refreshJobs = refreshJobs,
                stateHolder = stateHolder,
                scope = scope,
                isCurrentPage = isCurrentPage,
                onPageVisited = { tagId ->
                    visitedTagIds = (visitedTagIds + tagId).distinct()
                },
                onShowRefreshFailure = { tagId, message ->
                    messageJob?.cancel()
                    messageJob = scope.launch {
                        if (isCurrentPage(tagId)) showMessage(message)
                    }
                },
            )
        },
    )
}

@Composable
private fun NewsPagingCollector(
    tagId: Int,
    viewModel: NewsViewModel,
    pagingItemsByTag: SnapshotStateMap<Int, LazyPagingItems<NewsArticle>>,
) {
    val news = remember(viewModel, tagId) { viewModel.newsFor(tagId) }
    val newsItems = news.collectAsLazyPagingItems()
    DisposableEffect(tagId, pagingItemsByTag, newsItems) {
        pagingItemsByTag[tagId] = newsItems
        onDispose {
            if (pagingItemsByTag[tagId] === newsItems) pagingItemsByTag.remove(tagId)
        }
    }
}
