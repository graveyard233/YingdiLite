package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.graveyard.core.data.result.DataException
import com.graveyard.core.data.result.DataError
import com.graveyard.core.designsystem.component.CommonList
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.model.news.ArticleComment
import com.graveyard.core.model.news.ArticleReplyOrder
import com.graveyard.feature.news.ui.articleDetail.component.ArticleThreadCommentItem
import com.graveyard.feature.news.ui.newsList.component.NewsErrorState
import com.graveyard.feature.news.ui.newsList.component.newsErrorMessage
import com.graveyard.feature.news.viewmodel.ArticleRepliesUiState
import com.graveyard.feature.news.viewmodel.CommentVoteUiState
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_comment_reply
import yingdilite.feature.news.generated.resources.article_replies_back
import yingdilite.feature.news.generated.resources.article_replies_count
import yingdilite.feature.news.generated.resources.article_replies_empty
import yingdilite.feature.news.generated.resources.article_replies_load_failed
import yingdilite.feature.news.generated.resources.article_replies_loading
import yingdilite.feature.news.generated.resources.article_replies_order_ascending
import yingdilite.feature.news.generated.resources.article_replies_order_descending
import yingdilite.feature.news.generated.resources.article_replies_order_switch
import yingdilite.feature.news.generated.resources.article_replies_reply_author
import yingdilite.feature.news.generated.resources.article_replies_refresh_failed
import yingdilite.feature.news.generated.resources.article_replies_title
import yingdilite.feature.news.generated.resources.news_loading_more

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ArticleRepliesPage(
    commentId: Long,
    state: ArticleRepliesUiState,
    replies: LazyPagingItems<ArticleComment>,
    listState: LazyListState,
    onBack: () -> Unit,
    onLoad: () -> Unit,
    onOrderSelected: (ArticleReplyOrder) -> Unit,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    onLikeClicked: (comment: ArticleComment, parentCommentId: Long) -> Unit,
    onDislikeClicked: (comment: ArticleComment, parentCommentId: Long) -> Unit,
    onMoreClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    voteStates: Map<Long, CommentVoteUiState> = emptyMap(),
    snackbarHostState: SnackbarHostState,
) {
    val root = state.metadata?.mainComment
    val snapshot = replies.itemSnapshotList
    val refresh = replies.loadState.refresh
    val append = replies.loadState.append
    val hasReplies = snapshot.size > 0
    val error = state.error ?: (refresh as? LoadState.Error)?.let {
        (it.error as? DataException)?.error ?: DataError.Unknown
    }
    // A filtered page can be empty while Paging is still looking for visible replies.
    val exhausted = refresh is LoadState.NotLoading &&
        append is LoadState.NotLoading && append.endOfPaginationReached
    val retry = { if (state.feed == null) onLoad() else replies.retry() }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(Res.string.article_replies_title),
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (root != null) Text(
                            root.replyNum.toString(), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcon.ArrowBack.imageVector, stringResource(Res.string.article_replies_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            if (hasReplies && error != null) {
                CommentsRefreshError(retry, stringResource(Res.string.article_replies_refresh_failed))
            }
            CommonList(
                state = listState,
                isRefreshing = root != null && refresh is LoadState.Loading,
                onRefresh = { if (state.feed == null) onLoad() else replies.refresh() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                header = {
                    if (root != null) {
                        item(key = "root", contentType = "root") {
                            ArticleThreadCommentItem(
                                root, root.id, state.metadata?.isManager == true, true,
                                onImageClick, onLikeClicked, onDislikeClicked, onMoreClicked, voteStates,
                            )
                        }
                        item(key = "reply-heading", contentType = "heading") {
                            Box(
                                Modifier.fillMaxWidth().height(8.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    stringResource(Res.string.article_replies_count, root.replyNum),
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                RepliesOrderButton(state.order, onOrderSelected)
                            }
                        }
                    }
                },
                content = {
                    when {
                        !hasReplies && error != null -> item(key = "initial-error", contentType = "error") {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                NewsErrorState(stringResource(Res.string.article_replies_load_failed), error.newsErrorMessage(), retry)
                            }
                        }
                        hasReplies -> items(
                            count = snapshot.size,
                            key = { index -> snapshot.items.getOrNull(index)?.id?.let { "reply-$it" } ?: "placeholder-$index" },
                            contentType = { "reply" },
                        ) { index ->
                            if (replies.itemSnapshotList === snapshot) replies[index]
                            snapshot.items.getOrNull(index)?.let { reply ->
                                ArticleThreadCommentItem(
                                    reply, commentId, false, false,
                                    onImageClick, onLikeClicked, onDislikeClicked, onMoreClicked, voteStates,
                                )
                            }
                        }
                        append is LoadState.Error -> item(key = "filtered-append-error", contentType = "error") {
                            CommentsAppendError(append.error.newsErrorMessage(), retry)
                        }
                        root != null && refresh !is LoadState.Loading && append is LoadState.Loading ->
                            item(key = "filtered-append-loading", contentType = "append-loading") {
                                RepliesAppendLoading()
                            }
                        root != null && exhausted -> item(key = "empty", contentType = "empty") {
                            Text(
                                stringResource(Res.string.article_replies_empty),
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        else -> item(key = "loading", contentType = "loading") { RepliesSkeleton() }
                    }
                },
                footer = {
                    if (hasReplies) when (append) {
                        is LoadState.Loading -> item(key = "append-loading", contentType = "append-loading") {
                            RepliesAppendLoading()
                        }
                        is LoadState.Error -> item(key = "append-error", contentType = "append-error") {
                            CommentsAppendError(append.error.newsErrorMessage(), retry)
                        }
                        is LoadState.NotLoading -> Unit
                    }
                },
            )
        }
    }
}

@Composable
private fun RepliesOrderButton(
    order: ArticleReplyOrder,
    onOrderSelected: (ArticleReplyOrder) -> Unit,
) {
    val ascendingLabel = stringResource(Res.string.article_replies_order_ascending)
    val descendingLabel = stringResource(Res.string.article_replies_order_descending)
    val ascending = order == ArticleReplyOrder.Ascending
    val nextOrder = if (ascending) ArticleReplyOrder.Descending else ArticleReplyOrder.Ascending
    val label = if (ascending) ascendingLabel else descendingLabel
    val switchLabel = stringResource(
        Res.string.article_replies_order_switch,
        if (ascending) descendingLabel else ascendingLabel,
    )
    // Animate only the differing text: Chinese keeps "最" and "优先" stationary.
    val prefix = ascendingLabel.commonPrefixWith(descendingLabel)
    val ascendingRest = ascendingLabel.removePrefix(prefix)
    val descendingRest = descendingLabel.removePrefix(prefix)
    val suffix = ascendingRest.commonSuffixWith(descendingRest)
    val ascendingPart = ascendingRest.removeSuffix(suffix)
    val descendingPart = descendingRest.removeSuffix(suffix)

    TextButton(
        onClick = { onOrderSelected(nextOrder) },
        modifier = Modifier.heightIn(min = 48.dp).semantics {
            contentDescription = switchLabel
            stateDescription = label
        },
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        contentPadding = PaddingValues(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(
//            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(prefix, maxLines = 1)
                Box(contentAlignment = Alignment.Center) {
                    // Reserve the larger label's size, including at increased font scales.
                    Text(ascendingPart, modifier = Modifier.alpha(0f), maxLines = 1)
                    Text(descendingPart, modifier = Modifier.alpha(0f), maxLines = 1)
                    AnimatedContent(
                        targetState = order,
                        modifier = Modifier.matchParentSize().clipToBounds(),
                        contentAlignment = Alignment.Center,
                        transitionSpec = {
                            val direction = if (targetState == ArticleReplyOrder.Descending) 1 else -1
                            slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it * direction } togetherWith
                                slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { -it * direction }
                        },
                        label = "Reply order character",
                    ) { animatedOrder ->
                        Text(
                            if (animatedOrder == ArticleReplyOrder.Ascending) ascendingPart else descendingPart,
                            maxLines = 1,
                        )
                    }
                }
                Text(suffix, maxLines = 1)
            }
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = order, animationSpec = tween(180), label = "Reply order icon") { animatedOrder ->
                    Icon(
                        imageVector = if (animatedOrder == ArticleReplyOrder.Ascending)
                            AppIcon.SortAscending.imageVector else AppIcon.SortDescending.imageVector,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RepliesAppendLoading() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(stringResource(Res.string.news_loading_more), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RepliesSkeleton() {
    val description = stringResource(Res.string.article_replies_loading)
    Column(Modifier.fillMaxWidth().semantics { contentDescription = description }) {
        repeat(3) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
                    Box(Modifier.fillMaxWidth(0.4f).height(16.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh))
                }
                Box(Modifier.fillMaxWidth().height(16.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh))
                Box(Modifier.fillMaxWidth(0.7f).height(16.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh))
            }
        }
    }
}
