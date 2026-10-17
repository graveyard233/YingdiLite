package com.graveyard.feature.news.ui.newsList.screen

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import com.graveyard.core.model.news.NewsAuthor
import com.graveyard.feature.news.ui.newsList.component.NewsTagEditorSheet
import com.graveyard.feature.news.viewmodel.NewsTagEditor
import com.graveyard.feature.news.navigation.NewsPresentation
import com.graveyard.feature.news.navigation.LocalNewsPresentation
import com.graveyard.feature.news.viewmodel.NewsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.news_tags_save_failed

@Composable
fun NewsRouteScreen(
    onShowMessage: suspend (String) -> Unit,
    onArticleClick: (String, String, NewsAuthor) -> Unit,
    presentation: NewsPresentation = LocalNewsPresentation.current,
    viewModel: NewsViewModel = koinViewModel(),
) {
    // Capture this composition's list once. A delegated read in pageCount would see
    // a new list before the pager's key/content callbacks have recomposed.
    val categories = viewModel.categories.collectAsState().value
    val tagEditor = viewModel.tagEditor
    val saveFailureMessage by rememberUpdatedState(stringResource(Res.string.news_tags_save_failed))
    val categoryIds = remember(categories) {
        categories.map { it.id }
    }
    val selectedTagIds = remember(viewModel) {
        viewModel.uiState.map { it.selectedTagId }.distinctUntilChanged()
    }
    val selectedTagId by selectedTagIds.collectAsState(
        initial = viewModel.uiState.value.selectedTagId,
    )
    // A recreated ViewModel has no selection yet; restore the category by ID, not pager index.
    var savedTagId by rememberSaveable { mutableStateOf(selectedTagId) }
    val activeTagId = (selectedTagId ?: savedTagId)?.takeIf { it in categoryIds } ?: categoryIds.first()
    SideEffect { savedTagId = activeTagId }
    val currentTagId by rememberUpdatedState(activeTagId)
    val currentCategoryIds by rememberUpdatedState(categoryIds)
    val showMessage by rememberUpdatedState(onShowMessage)
    LaunchedEffect(tagEditor) {
        tagEditor.messages.collect { showMessage(saveFailureMessage) }
    }
    val stateHolder = rememberSaveableStateHolder()
    val pagerState = rememberPagerState(
        initialPage = categoryIds.indexOf(activeTagId),
        pageCount = { categories.size },
    )
    val scope = rememberCoroutineScope()
    val pagingItemsByTag = remember(viewModel) {
        mutableStateMapOf<Int, LazyPagingItems<NewsArticle>>()
    }
    val refreshingTags by viewModel.refreshingTags.collectAsState()
    val currentPresentation by rememberUpdatedState(presentation)
    LaunchedEffect(presentation.interactive, presentation.listVisible) {
        if (!presentation.interactive || !presentation.listVisible) tagEditor.close()
    }
    var messageJob by remember { mutableStateOf<Job?>(null) }
    var visitedTagIds by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var previousCategoryIds by rememberSaveable { mutableStateOf(categoryIds) }
    var pagerCategoryIds by remember { mutableStateOf(emptyList<Int>()) }
    var isSynchronizingPager by remember { mutableStateOf(true) }
    var requestedTagId by remember { mutableStateOf<Int?>(null) }
    var pagerNavigationRequest by remember { mutableStateOf(0) }

    val isCurrentPage: (Int) -> Boolean = { tagId ->
        currentPresentation.interactive && currentPresentation.listVisible && currentTagId == tagId &&
            !isSynchronizingPager &&
            !pagerState.isScrollInProgress &&
            currentCategoryIds.getOrNull(pagerState.settledPage) == tagId
    }
    val onSelectTag: (Int) -> Unit = { tagId ->
        messageJob?.cancel()
        isSynchronizingPager = true
        requestedTagId = tagId
        pagerNavigationRequest++
        visitedTagIds = (visitedTagIds + tagId).distinct()
        viewModel.selectTag(tagId)
    }

    LaunchedEffect(categoryIds) {
        val removedIds = previousCategoryIds.filter { it !in categoryIds }
        removedIds.forEach { tagId ->
            pagingItemsByTag.remove(tagId)
            stateHolder.removeState(tagId)
        }
        visitedTagIds = visitedTagIds.filter { it in categoryIds }
        previousCategoryIds = categoryIds
        viewModel.selectTag(activeTagId)
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
        onManageTags = tagEditor::open,
        onSelectCategory = { category -> onSelectTag(category.id) },
        pageContent = { category ->
            NewsCategoryPage(
                tagId = category.id,
                viewModel = viewModel,
                pagingItemsByTag = pagingItemsByTag,
                isRefreshing = category.id in refreshingTags,
                stateHolder = stateHolder,
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
                onArticleClick = { id, title, newsAuthor ->
                    if (currentPresentation.interactive) onArticleClick(id, title, newsAuthor)
                },
                selectedArticleId = presentation.selectedArticleId,
            )
        },
    )

    if (presentation.interactive && presentation.listVisible) NewsTagEditorHost(
        tagEditor = tagEditor,
        onRefresh = viewModel::getTagInfos,
        onSelectTag = onSelectTag,
    )
}

@Composable
private fun NewsTagEditorHost(
    tagEditor: NewsTagEditor,
    onRefresh: () -> Unit,
    onSelectTag: (Int) -> Unit,
) {
    val state by tagEditor.state.collectAsState()
    // The host owns observation for the route lifetime. A sheet's visibility
    // transition must not dispose an old close callback after a rapid reopening.
    DisposableEffect(tagEditor) {
        onDispose { tagEditor.close() }
    }
    if (state.isVisible) {
        NewsTagEditorSheet(
            state = state,
            onDismiss = tagEditor::close,
            onRetryInitialization = tagEditor::retryInitialization,
            onRefresh = {
                val current = tagEditor.state.value
                if (current.canAddOrRemove && !current.isRefreshing) {
                    onRefresh()
                }
            },
            onSelect = { tagId ->
                val current = tagEditor.state.value
                if (current.canAddOrRemove && current.userTags.any { it.id == tagId }) {
                    onSelectTag(tagId)
                    tagEditor.close()
                }
            },
            onAdd = tagEditor::add,
            onRemove = tagEditor::remove,
            onDragStarted = tagEditor::startDrag,
            onMove = tagEditor::move,
            onDragStopped = tagEditor::finishDrag,
            onMoveBy = tagEditor::moveBy,
        )
    }
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
