package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.graveyard.core.data.repository.ArticleVote
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException
import com.graveyard.core.designsystem.component.CommonList
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.model.news.ArticleCommentOrder
import com.graveyard.core.model.news.ArticleComment
import com.graveyard.core.model.news.ArticleCommentThread
import com.graveyard.feature.news.ui.articleDetail.component.ArticleCommentItem
import com.graveyard.feature.news.ui.articleDetail.component.ArticleCommentsBottomBar
import com.graveyard.feature.news.ui.articleDetail.component.ArticleCommentsBottomBarHeight
import com.graveyard.feature.news.ui.newsList.component.NewsErrorState
import com.graveyard.feature.news.ui.newsList.component.newsErrorMessage
import com.graveyard.feature.news.viewmodel.ArticleCommentsUiState
import com.graveyard.feature.news.viewmodel.CommentVoteUiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_back_to_detail
import yingdilite.feature.news.generated.resources.article_comments_empty_message
import yingdilite.feature.news.generated.resources.article_comments_empty_title
import yingdilite.feature.news.generated.resources.article_comments_hot_count
import yingdilite.feature.news.generated.resources.article_comments_hot_section
import yingdilite.feature.news.generated.resources.article_comments_load_failed
import yingdilite.feature.news.generated.resources.article_comments_normal_section
import yingdilite.feature.news.generated.resources.article_comments_refresh_failed
import yingdilite.feature.news.generated.resources.article_comments_title
import yingdilite.feature.news.generated.resources.news_load_more_failed
import yingdilite.feature.news.generated.resources.news_loading_more
import yingdilite.feature.news.generated.resources.news_retry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleCommentsPage(
    commentsState: ArticleCommentsUiState,
    comments: LazyPagingItems<ArticleCommentThread>,
    listState: LazyListState,
    likeNum: String,
    onBack: () -> Unit,
    vote: ArticleVote = ArticleVote.None,
    showBack: Boolean = true,
    interactionEnabled: Boolean = true,
    isVoteCoolingDown: Boolean = false,
    voteCooldownProgress: () -> Float = { 1f },
    onFireClick: (SnackbarHostState) -> Unit = {},
    onHateClick: (SnackbarHostState) -> Unit = {},
    onLoadComments: () -> Unit,
    onOrderSelected: (ArticleCommentOrder) -> Unit,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    onLikeClicked: (comment: ArticleComment, parentCommentId: Long) -> Unit,
    onDislikeClicked: (comment: ArticleComment, parentCommentId: Long) -> Unit,
    onMoreClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onViewReplies: (commentId: Long) -> Unit,
    voteStates: Map<Long, CommentVoteUiState> = emptyMap(),
    snackbarHostState: SnackbarHostState,
) {
    val coroutineScope = rememberCoroutineScope()
    val canScroll by remember(listState) {
        derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
    }
    val bottomBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior(
        canScroll = { canScroll },
        snapAnimationSpec = tween(150),
    )
    LaunchedEffect(canScroll) {
        if (!canScroll) {
            bottomBarScrollBehavior.state.heightOffset = 0f
            bottomBarScrollBehavior.state.contentOffset = 0f
        }
    }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    vote = vote,
                    interactionEnabled = interactionEnabled,
                    isVoteCoolingDown = isVoteCoolingDown,
                    voteCooldownProgress = voteCooldownProgress,
                    commentOrder = commentsState.order,
                    onOrderSelected = onOrderSelected,
                    onToTopClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                            bottomBarScrollBehavior.state.heightOffset = 0f
                            bottomBarScrollBehavior.state.contentOffset = 0f
                        }
                    },
                    onFireClick = { onFireClick(snackbarHostState) },
                    onHateClick = { onHateClick(snackbarHostState) },
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
        val loadState = comments.loadState
        val refreshState = loadState.refresh
        val appendState = loadState.append
        // Count, keys and rows share one snapshot so a shorter refresh cannot invalidate old indices.
        val commentsSnapshot = comments.itemSnapshotList
        val hotComments = if (commentsState.order == ArticleCommentOrder.Hot) {
            commentsState.metadata?.hotComments.orEmpty()
        } else emptyList()
        val normalCount = commentsSnapshot.size
        val showSectionBreak = hotComments.isNotEmpty() && normalCount > 0
        val bodyState = resolveCommentsBodyState(
            commentsState = commentsState,
            hasPresentedComments = hotComments.isNotEmpty() || normalCount > 0,
            refreshState = refreshState,
            appendState = appendState,
        )
        val hasCommentsSession = commentsState.feed != null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .clipToBounds(),
        ) {
            if (bodyState == CommentsBodyState.Content && refreshState is LoadState.Error) {
                CommentsRefreshError(onRetry = { comments.retry() })
            }
            CommonList(
                state = listState,
                isRefreshing = bodyState == CommentsBodyState.Content && refreshState is LoadState.Loading,
                onRefresh = { if (hasCommentsSession) comments.refresh() else onLoadComments() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = ArticleCommentsBottomBarHeight + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                header = {
                    if (bodyState == CommentsBodyState.Content) {
                        if (hotComments.isNotEmpty()) {
                            item(key = "hot-comments-heading", contentType = "hot-heading") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                        .padding(top = 4.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.article_comments_hot_section),
                                        modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = stringResource(
                                            Res.string.article_comments_hot_count,
                                            hotComments.size,
                                        ),
                                        modifier = Modifier.alignByBaseline(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        items(hotComments, key = { "hot-${it.mainComment.id}" }, contentType = { "comment" }) { thread ->
                            ArticleCommentItem(
                                thread, onImageClick, onLikeClicked, onDislikeClicked, onMoreClicked, onViewReplies,
                                showDivider = !showSectionBreak || thread.mainComment.id != hotComments.last().mainComment.id,
                                voteStates = voteStates,
                            )
                        }
                        if (normalCount > 0) {
                            item(key = "normal-comments-heading", contentType = "normal-heading") {
                                Column {
                                    if (showSectionBreak) {
                                        Box(
                                            Modifier.fillMaxWidth().height(8.dp)
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        )
                                    }
                                    Text(
                                        text = stringResource(Res.string.article_comments_normal_section),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                            .padding(top = if (showSectionBreak) 12.dp else 4.dp, bottom = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                },
                content = {
                    when (bodyState) {
                        CommentsBodyState.Loading -> item(key = "comments-loading", contentType = "body-loading") {
                            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        }
                        is CommentsBodyState.Error -> item(key = "comments-initial-error", contentType = "body-error") {
                            Box(Modifier.fillParentMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                NewsErrorState(
                                    title = stringResource(Res.string.article_comments_load_failed),
                                    message = bodyState.error.newsErrorMessage(),
                                    onRetry = { if (hasCommentsSession) comments.retry() else onLoadComments() },
                                )
                            }
                        }
                        CommentsBodyState.Empty -> item(key = "comments-empty", contentType = "body-empty") {
                            ArticleCommentsEmptyState(Modifier.fillParentMaxSize())
                        }
                        CommentsBodyState.Content -> {
                            items(
                                count = normalCount,
                                contentType = { "comment" },
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
                                    ArticleCommentItem(thread, onImageClick, onLikeClicked, onDislikeClicked, onMoreClicked, onViewReplies, voteStates = voteStates)
                                }
                            }
                        }
                    }
                },
                footer = {
                    if (bodyState == CommentsBodyState.Content) {
                        when (val state = appendState) {
                            LoadState.Loading -> item(key = "comments-append-loading", contentType = "append-loading") {
                                Text(
                                    text = stringResource(Res.string.news_loading_more),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            is LoadState.Error -> item(key = "comments-append-error", contentType = "append-error") {
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

private sealed interface CommentsBodyState {
    data object Loading : CommentsBodyState
    data class Error(val error: DataError) : CommentsBodyState
    data object Empty : CommentsBodyState
    data object Content : CommentsBodyState
}

private fun resolveCommentsBodyState(
    commentsState: ArticleCommentsUiState,
    hasPresentedComments: Boolean,
    refreshState: LoadState,
    appendState: LoadState,
): CommentsBodyState = when {
    hasPresentedComments -> CommentsBodyState.Content
    commentsState.error != null -> CommentsBodyState.Error(commentsState.error)
    refreshState is LoadState.Error -> CommentsBodyState.Error(refreshState.error.commentDataError())
    commentsState.feed == null || commentsState.metadata == null || refreshState is LoadState.Loading ->
        CommentsBodyState.Loading
    appendState is LoadState.Error -> CommentsBodyState.Error(appendState.error.commentDataError())
    refreshState is LoadState.NotLoading && appendState is LoadState.NotLoading && appendState.endOfPaginationReached ->
        CommentsBodyState.Empty
    // Empty filtered pages can still be followed by visible comments.
    else -> CommentsBodyState.Loading
}

private fun Throwable.commentDataError(): DataError = (this as? DataException)?.error ?: DataError.Unknown

@Composable
private fun ArticleCommentsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Icon(AppIcon.Comment.imageVector, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(Res.string.article_comments_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.article_comments_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun CommentsRefreshError(
    onRetry: () -> Unit,
    message: String = stringResource(Res.string.article_comments_refresh_failed),
) {
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
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.TextButton(onClick = onRetry) { Text(stringResource(Res.string.news_retry)) }
        }
    }
}

@Composable
internal fun CommentsAppendError(message: String, onRetry: () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
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
