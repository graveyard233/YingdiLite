package com.graveyard.feature.news.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.model.news.NewsArticle
import com.graveyard.feature.news.model.NewsCategory
import com.graveyard.feature.news.viewmodel.BannerUiState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    categories: List<NewsCategory>,
    pagerState: PagerState,
    onSelectCategory: (NewsCategory) -> Unit,
    pageContent: @Composable (NewsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val selectedTabIndex = if (categories.isEmpty()) {
        0
    } else {
        pagerState.currentPage.coerceIn(categories.indices)
    }

    Scaffold(
        modifier = modifier,
        containerColor = colors.surfaceContainerLowest,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = selectedTabIndex,
                            edgePadding = 16.dp,
                            divider = {},
                        ) {
                            categories.forEachIndexed { index, category ->
                                Tab(
                                    selected = index == selectedTabIndex,
                                    onClick = { onSelectCategory(category) },
                                    text = { Text(category.label) },
                                )
                            }
                        }
                    },
                    actions = {
                        UnavailableNewsAction(AppIcon.Search, "搜索")
                    },
                )

            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> categories[page].id },
            ) { page ->
                pageContent(categories[page])
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
internal fun NewsPage(
    bannerState: BannerUiState,
    newsItems: LazyPagingItems<NewsArticle>?,
    listState: LazyListState,
    isRefreshing: Boolean,
    onRetryBanners: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nowEpochSeconds = Clock.System.now().epochSeconds

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                bannerState.items.isNotEmpty() -> item(
                    key = "banners",
                    contentType = "banners",
                ) {
                    NewsBannerCarousel(bannerState.items)
                }
                bannerState.isLoading -> item(
                    key = "banners",
                    contentType = "banner-placeholder",
                ) {
                    BannerPlaceholder()
                }
                bannerState.error != null -> item(
                    key = "banner-error",
                    contentType = "status",
                ) {
                    NewsErrorState(
                        title = "推荐内容加载失败",
                        message = bannerState.error.newsErrorMessage(),
                        onRetry = onRetryBanners,
                        compact = true,
                    )
                }
            }

            val refresh = newsItems?.loadState?.refresh ?: LoadState.Loading
            if (newsItems == null || newsItems.itemCount == 0) {
                when (refresh) {
                    LoadState.Loading -> items(
                        count = 4,
                        key = { "news-placeholder-$it" },
                        contentType = { "news-placeholder" },
                    ) {
                        NewsArticlePlaceholder()
                    }
                    is LoadState.Error -> item(
                        key = "news-error",
                        contentType = "status",
                    ) {
                        NewsErrorState(
                            title = "新闻加载失败",
                            message = refresh.error.newsErrorMessage(),
                            onRetry = { newsItems?.retry() },
                        )
                    }
                    is LoadState.NotLoading -> item(
                        key = "news-empty",
                        contentType = "status",
                    ) {
                        NewsEmptyState()
                    }
                }
            } else {
                items(
                    count = newsItems.itemCount,
                    key = newsItems.itemKey { "news-${it.id}" },
                    contentType = { "news" },
                ) { index ->
                    newsItems[index]?.let { article ->
                        NewsArticleItem(article, nowEpochSeconds)
                    }
                }
                // Existing-content refresh errors are reported by the route's Snackbar.
                if (refresh is LoadState.NotLoading) {
                    item(key = "news-footer", contentType = "status") {
                        NewsPagingFooter(newsItems.loadState.append, newsItems::retry)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnavailableNewsAction(icon: AppIcon, label: String) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = {}, enabled = false) {
            Icon(imageVector = icon.imageVector, contentDescription = label)
        }
    }
}
