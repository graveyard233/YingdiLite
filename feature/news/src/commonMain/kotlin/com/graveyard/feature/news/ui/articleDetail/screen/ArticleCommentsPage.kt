package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.graveyard.core.designsystem.component.CommonList
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.model.news.ArticleCommentOrder
import com.graveyard.core.model.news.ArticleCommentThread
import com.graveyard.core.model.news.ArticleCommentsMetadata
import com.graveyard.feature.news.ui.articleDetail.component.ArticleCommentCard
import com.graveyard.feature.news.ui.articleDetail.component.ArticleCommentsBottomBar
import com.graveyard.feature.news.ui.articleDetail.component.ArticleCommentsBottomBarHeight
import com.graveyard.feature.news.ui.newsList.component.NewsErrorState
import com.graveyard.feature.news.ui.newsList.component.newsErrorMessage
import com.graveyard.feature.news.viewmodel.ArticleUiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_back_to_detail
import yingdilite.feature.news.generated.resources.article_comments_empty_message
import yingdilite.feature.news.generated.resources.article_comments_empty_title
import yingdilite.feature.news.generated.resources.article_comments_hot_section
import yingdilite.feature.news.generated.resources.article_comments_load_failed
import yingdilite.feature.news.generated.resources.article_comments_normal_section
import yingdilite.feature.news.generated.resources.article_comments_refresh_failed
import yingdilite.feature.news.generated.resources.article_comments_sorted_empty_message
import yingdilite.feature.news.generated.resources.article_comments_sorted_empty_title
import yingdilite.feature.news.generated.resources.article_comments_title
import yingdilite.feature.news.generated.resources.news_load_more_failed
import yingdilite.feature.news.generated.resources.news_loading_more
import yingdilite.feature.news.generated.resources.news_retry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleCommentsPage(
    uiState: ArticleUiState,
    hasCommentsSession: Boolean,
    commentsMetadata: ArticleCommentsMetadata?,
    commentOrder: ArticleCommentOrder,
    comments: LazyPagingItems<ArticleCommentThread>,
    listState: LazyListState,
    likeNum: String,
    onBack: () -> Unit,
    showBack: Boolean = true,
    onLoadComments: () -> Unit,
    onOrderSelected: (ArticleCommentOrder) -> Unit,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    onLikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onDislikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onMoreClicked: (commentId: Long, parentCommentId: Long) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val bottomBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior(
        canScroll = { listState.canScrollForward || listState.canScrollBackward },
        snapAnimationSpec = tween(150),
    )
    LaunchedEffect(listState.canScrollForward, listState.canScrollBackward) {
        if (!listState.canScrollForward && !listState.canScrollBackward) {
            bottomBarScrollBehavior.state.heightOffset = 0f
            bottomBarScrollBehavior.state.contentOffset = 0f
        }
    }
    val refreshState = comments.loadState.refresh
    // Capture the presented snapshot once. The count, the item keys and the rendered rows must all
    // come from this same instance: after a refresh replaces the feed with a shorter page, lazy
    // items built from the old count are still measured, and reading the live list with one of
    // their indices throws IndexOutOfBoundsException.
    val commentsSnapshot = comments.itemSnapshotList
    // Hot comments and counts arrive with the first page, but travel next to the list instead of
    // inside it. An empty feed can therefore still show its hot section.
    val hotComments = if (commentOrder == ArticleCommentOrder.Hot) commentsMetadata?.hotComments.orEmpty() else emptyList()
    val normalCount = commentsSnapshot.size
    val hasPresentedComments = hotComments.isNotEmpty() || normalCount > 0
    val refreshError = refreshState as? LoadState.Error
    val appendState = comments.loadState.append
    val notStarted = !hasCommentsSession && uiState.commentsError == null
    // Metadata is published right before Paging delivers the page, so a refresh that is still
    // loading keeps the content slot until the list itself has something to present.
    val awaitingFirstPage = hasCommentsSession && refreshError == null &&
        (commentsMetadata == null || (refreshState is LoadState.Loading && !hasPresentedComments))
    val initialLoading = notStarted || awaitingFirstPage
    val initialError = (refreshError != null && !hasPresentedComments && commentsMetadata == null) ||
        (!hasCommentsSession && uiState.commentsError != null)
    val showEmpty = commentsMetadata != null && !hasPresentedComments && !initialLoading && !initialError
    val isSortedEmpty = commentOrder == ArticleCommentOrder.Created && (commentsMetadata?.hotNum ?: 0) > 0

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(bottomBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.article_comments_title)) },
                navigationIcon = {
                    if (showBack) IconButton(onClick = onBack) {
                        Icon(AppIcon.ArrowBack.imageVector, stringResource(Res.string.article_back_to_detail))
                    }
                },
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BottomAppBarDefaults.containerColor)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .clipToBounds(),
            ) {
                ArticleCommentsBottomBar(
                    likeNum = likeNum,
                    commentOrder = commentOrder,
                    onOrderSelected = onOrderSelected,
                    onToTopClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                            bottomBarScrollBehavior.state.heightOffset = 0f
                            bottomBarScrollBehavior.state.contentOffset = 0f
                        }
                    },
                    onFireClick = {
                        // TODO: Connect the article heat action.
                    },
                    scrollBehavior = bottomBarScrollBehavior,
                )
            }
        },
    ) { scaffoldPadding ->
        val layoutDirection = LocalLayoutDirection.current
        // Overlay the collapsing bar so its height changes do not resize the scrolling list.
        val contentPadding = PaddingValues(
            start = scaffoldPadding.calculateStartPadding(layoutDirection),
            top = scaffoldPadding.calculateTopPadding(),
            end = scaffoldPadding.calculateEndPadding(layoutDirection),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .clipToBounds(),
        ) {
            if (refreshError != null && commentsMetadata != null) {
                CommentsRefreshError(onRetry = { comments.retry() })
            }
            CommonList(
                state = listState,
                isRefreshing = refreshState is LoadState.Loading && (hasPresentedComments || commentsMetadata != null),
                onRefresh = { if (hasCommentsSession) comments.refresh() else onLoadComments() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = ArticleCommentsBottomBarHeight + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                header = {
                    if (hotComments.isNotEmpty()) {
                        item(key = "hot-comments-heading") {
                            Text(
                                text = stringResource(Res.string.article_comments_hot_section),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
                            )
                        }
                    }
                    items(hotComments, key = { "hot-${it.mainComment.id}" }) { thread ->
                        ArticleCommentCard(thread, onImageClick, onLikeClicked, onDislikeClicked, onMoreClicked)
                    }
                    if (normalCount > 0) {
                        item(key = "normal-comments-heading") {
                            Text(
                                text = stringResource(Res.string.article_comments_normal_section),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                            )
                        }
                    }
                },
                content = {
                    when {
                        initialLoading -> item(key = "comments-loading") {
                            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        }
                        initialError -> item(key = "comments-initial-error") {
                            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                NewsErrorState(
                                    title = stringResource(Res.string.article_comments_load_failed),
                                    message = refreshError?.error?.newsErrorMessage()
                                        ?: uiState.commentsError?.newsErrorMessage().orEmpty(),
                                    onRetry = { if (hasCommentsSession) comments.retry() else onLoadComments() },
                                )
                            }
                        }
                        showEmpty -> item(key = "comments-empty") {
                            ArticleCommentsEmptyState(isSortedEmpty, Modifier.fillParentMaxSize())
                        }
                        else -> {
                            items(
                                count = normalCount,
                                key = { index ->
                                    // Keys must stay stable while Paging replaces its snapshot, so
                                    // they are derived from the captured snapshot, never the live one.
                                    commentsSnapshot.items.getOrNull(index)?.mainComment?.id
                                        ?.let { "normal-$it" }
                                        ?: "normal-placeholder-$index"
                                },
                            ) { index ->
                                // Report the viewport only while this row still belongs to the
                                // presented snapshot; a replacement snapshot may be shorter.
                                if (comments.itemSnapshotList === commentsSnapshot) {
                                    comments[index]
                                }
                                commentsSnapshot.items.getOrNull(index)?.let { thread ->
                                    ArticleCommentCard(thread, onImageClick, onLikeClicked, onDislikeClicked, onMoreClicked)
                                }
                            }
                        }
                    }
                },
                footer = {
                    if (hasPresentedComments) {
                        when (val state = appendState) {
                            LoadState.Loading -> item(key = "comments-append-loading") {
                                Text(
                                    text = stringResource(Res.string.news_loading_more),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            is LoadState.Error -> item(key = "comments-append-error") {
                                CommentsAppendError(state.error.newsErrorMessage(), onRetry = { comments.retry() })
                            }
                            is LoadState.NotLoading -> Unit
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ArticleCommentsEmptyState(isSortedEmpty: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Icon(AppIcon.Comment.imageVector, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(if (isSortedEmpty) Res.string.article_comments_sorted_empty_title else Res.string.article_comments_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(if (isSortedEmpty) Res.string.article_comments_sorted_empty_message else Res.string.article_comments_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CommentsRefreshError(onRetry: () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.article_comments_refresh_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.TextButton(onClick = onRetry) { Text(stringResource(Res.string.news_retry)) }
        }
    }
}

@Composable
private fun CommentsAppendError(message: String, onRetry: () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(Res.string.news_load_more_failed), style = MaterialTheme.typography.titleSmall)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.material3.TextButton(onClick = onRetry) { Text(stringResource(Res.string.news_retry)) }
        }
    }
}
