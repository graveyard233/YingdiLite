package com.graveyard.feature.news.ui.articleDetail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.graveyard.core.data.repository.ArticleVote
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.model.news.ArticleCommentOrder
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_comments_order_checked
import yingdilite.feature.news.generated.resources.article_comments_write_short
import yingdilite.feature.news.generated.resources.article_comments_order_created
import yingdilite.feature.news.generated.resources.article_comments_order_hot
import yingdilite.feature.news.generated.resources.article_fire_count
import yingdilite.feature.news.generated.resources.article_hate
import yingdilite.feature.news.generated.resources.article_to_top

internal val ArticleCommentsBottomBarHeight = 56.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ArticleCommentsBottomBar(
    likeNum: String,
    vote: ArticleVote,
    commentOrder: ArticleCommentOrder,
    onOrderSelected: (ArticleCommentOrder) -> Unit,
    onToTopClick: () -> Unit,
    onFireClick: () -> Unit,
    onHateClick: () -> Unit,
    scrollBehavior: BottomAppBarScrollBehavior,
) {
    val missingCount = "—"
    val fire = likeNum.ifBlank { missingCount }
    val fireDescription = stringResource(Res.string.article_fire_count, fire)
    val liked = vote == ArticleVote.Like
    val hated = vote == ArticleVote.Hate
    val interactive = scrollBehavior.state.collapsedFraction < 1f
    FlexibleBottomAppBar(
        modifier = Modifier.fillMaxWidth()
            .then(if (interactive) Modifier else Modifier.clearAndSetSemantics {}),
        expandedHeight = ArticleCommentsBottomBarHeight,
        horizontalArrangement = Arrangement.Start,
        contentPadding = PaddingValues(0.dp),
        windowInsets = WindowInsets(0, 0, 0, 0),
        scrollBehavior = scrollBehavior,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.3f
            val gutter = if (compact) 8.dp else 16.dp
            val gap = if (compact) 0.dp else 4.dp
            // Three fixed 48dp buttons (to top, hate, write) plus the gaps between actions.
            val actionWidth = (maxWidth - gutter * 2 - 144.dp - gap * 2 - 8.dp)
                .coerceIn(48.dp, 80.dp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToTopClick, enabled = interactive, modifier = Modifier.size(48.dp)) {
                    Icon(AppIcon.ToTop.imageVector, stringResource(Res.string.article_to_top))
                }
                Spacer(Modifier.width(gap))
                CommentOrderMenu(
                    order = commentOrder,
                    enabled = interactive,
                    onOrderSelected = onOrderSelected,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onFireClick,
                        enabled = interactive,
                        modifier = Modifier
                            .widthIn(min = 48.dp, max = actionWidth)
                            .heightIn(min = 48.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = fireDescription
                                selected = liked
                            },
                        // TextButton defaults to primary; inactive fire matches the other bar icons.
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = (if (liked) AppIcon.FireFill else AppIcon.Fire).imageVector,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = fire,
                            modifier = Modifier.weight(1f, fill = false).clearAndSetSemantics {},
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = onHateClick,
                        enabled = interactive,
                        modifier = Modifier.size(48.dp).semantics { selected = hated },
                    ) {
                        Icon(
                            imageVector = (if (hated) AppIcon.ThumbDownFill else AppIcon.ThumbDown).imageVector,
                            contentDescription = stringResource(Res.string.article_hate),
                        )
                    }
                    IconButton(
                        onClick = { /* TODO: Connect the comment composer. */ },
                        enabled = false,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(AppIcon.Edit.imageVector, stringResource(Res.string.article_comments_write_short))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentOrderMenu(
    order: ArticleCommentOrder,
    enabled: Boolean,
    onOrderSelected: (ArticleCommentOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(order) { mutableStateOf(false) }
    val hotLabel = stringResource(Res.string.article_comments_order_hot)
    val createdLabel = stringResource(Res.string.article_comments_order_created)
    LaunchedEffect(enabled) {
        if (!enabled) expanded = false
    }
    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = !expanded },
            enabled = enabled,
            modifier = Modifier.heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                text = if (order == ArticleCommentOrder.Hot) hotLabel else createdLabel,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(4.dp))
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            properties = PopupProperties(
                focusable = true,
                clippingEnabled = false,
            ),
        ) {
            ArticleCommentOrder.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(if (option == ArticleCommentOrder.Hot) hotLabel else createdLabel) },
                    onClick = {
                        expanded = false
                        if (option != order) onOrderSelected(option)
                    },
                    leadingIcon = {
                        if (option == order) {
                            Icon(AppIcon.Check.imageVector,stringResource(Res.string.article_comments_order_checked))
                        }
                    },
                )
            }
        }
    }
}
