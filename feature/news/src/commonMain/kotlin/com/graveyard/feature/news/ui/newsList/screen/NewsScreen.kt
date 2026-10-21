package com.graveyard.feature.news.ui.newsList.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.designsystem.component.CommonList
import com.graveyard.core.model.news.NewsArticle
import com.graveyard.core.model.news.TagInfo
import com.graveyard.feature.news.ui.newsList.component.BannerPlaceholder
import com.graveyard.feature.news.ui.newsList.component.BannerRefreshFailureNotice
import com.graveyard.feature.news.ui.newsList.component.NewsArticleItem
import com.graveyard.feature.news.ui.newsList.component.NewsArticlePlaceholder
import com.graveyard.feature.news.ui.newsList.component.NewsBannerCarousel
import com.graveyard.feature.news.ui.newsList.component.NewsEmptyState
import com.graveyard.feature.news.ui.newsList.component.NewsErrorState
import com.graveyard.feature.news.ui.newsList.component.NewsPagingFooter
import com.graveyard.feature.news.ui.newsList.component.NewsRefreshFailureBar
import com.graveyard.feature.news.ui.newsList.component.newsErrorMessage
import com.graveyard.feature.news.viewmodel.BannerUiState
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.news_banners_load_failed
import yingdilite.feature.news.generated.resources.news_banners_stale
import yingdilite.feature.news.generated.resources.news_content_load_failed
import yingdilite.feature.news.generated.resources.news_load_failed
import yingdilite.feature.news.generated.resources.news_manage_tags
import yingdilite.feature.news.generated.resources.news_refresh_failed_keep_content
import yingdilite.feature.news.generated.resources.news_search
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    categories: List<TagInfo>,
    pagerState: PagerState,
    onManageTags: () -> Unit,
    onSelectCategory: (TagInfo) -> Unit,
    pageContent: @Composable (TagInfo) -> Unit,
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
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier,
                            onClick = onManageTags,
                        ) {
                            Icon(
                                imageVector = AppIcon.Menu.imageVector,
                                contentDescription = stringResource(Res.string.news_manage_tags),
                            )
                        }
                    },
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
                                    text = { Text(category.title) },
                                )
                            }
                        }
                    },
                    actions = {
                        UnavailableNewsAction(
                            AppIcon.Search,
                            stringResource(Res.string.news_search),
                        )
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
                // A previous layout/prefetch may briefly retain an index after tags change.
                // String keys cannot collide with the valid tags' Int IDs.
                key = { page -> categories.getOrNull(page)?.id ?: "news-page-$page" },
            ) { page ->
                categories.getOrNull(page)?.let { category ->
                    pageContent(category)
                }
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
    onRetryBanners: () -> Unit,
    onRefresh: () -> Unit,
    onArticleClick: (String, String, String, String) -> Unit,
    selectedArticleId: String? = null,
    modifier: Modifier = Modifier,
) {
    val nowEpochSeconds = Clock.System.now().epochSeconds

    val refresh = newsItems?.loadState?.refresh ?: LoadState.Loading
    val hasNews = newsItems != null && newsItems.itemCount > 0
    val refreshError = (refresh as? LoadState.Error)?.error
    val isRefreshing = hasNews && refresh is LoadState.Loading
    val pageError = (refresh as? LoadState.Error)?.takeIf {
        !hasNews && bannerState.items.isEmpty()
    }
    var refreshErrorDismissed by remember(refreshError) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        if (hasNews && refreshError != null && !refreshErrorDismissed) {
            NewsRefreshFailureBar(
                message = stringResource(Res.string.news_refresh_failed_keep_content),
                onRetry = { newsItems.retry() },
                onDismiss = { refreshErrorDismissed = true },
            )
        }

        CommonList(
            state = listState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            header = {
                when {
                    bannerState.items.isNotEmpty() -> item(
                        key = "banners",
                        contentType = "banners",
                    ) {
                        Column {
                            NewsBannerCarousel(bannerState.items)
                            if (bannerState.error != null) {
                                BannerRefreshFailureNotice(
                                    message = stringResource(Res.string.news_banners_stale),
                                    onRetry = onRetryBanners,
                                )
                            }
                        }
                    }
                    pageError != null -> Unit
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
                            title = stringResource(Res.string.news_banners_load_failed),
                            message = bannerState.error.newsErrorMessage(),
                            onRetry = onRetryBanners,
                            compact = true,
                        )
                    }
                }
            },
            content = {
                if (!hasNews) {
                    if (pageError != null) {
                        item(key = "page-error", contentType = "status") {
                            NewsErrorState(
                                title = stringResource(Res.string.news_content_load_failed),
                                message = pageError.error.newsErrorMessage(),
                                onRetry = {
                                    newsItems?.retry()
                                    if (bannerState.error != null) onRetryBanners()
                                },
                            )
                        }
                    } else {
                        when (refresh) {
                            LoadState.Loading -> items(
                                count = 5,
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
                                    title = stringResource(Res.string.news_load_failed),
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
                    }
                } else {
                    items(
                        count = newsItems.itemCount,
                        key = newsItems.itemKey { "news-${it.id}" },
                        contentType = { "news" },
                    ) { index ->
                        newsItems[index]?.let { article ->
                            NewsArticleItem(
                                article = article,
                                selected = article.sourceId == selectedArticleId,
                                nowEpochSeconds = nowEpochSeconds,
                                onClick = {
                                    onArticleClick(
                                        article.sourceId,
                                        article.title,
                                        article.replyNum,
                                        article.likeNum,
                                    )
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            },
            footer = {
                // Existing-content refresh errors are reported by the bar above.
                val items = newsItems
                if (items != null && items.itemCount > 0 && refresh is LoadState.NotLoading) {
                    item(key = "news-footer", contentType = "status") {
                        NewsPagingFooter(items.loadState.append, items::retry)
                    }
                }
            },
        )
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
