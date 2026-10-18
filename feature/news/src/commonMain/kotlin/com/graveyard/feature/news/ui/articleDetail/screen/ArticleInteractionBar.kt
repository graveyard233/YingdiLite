package com.graveyard.feature.news.ui.articleDetail.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.graveyard.core.designsystem.icons.AppIcon
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_favorite
import yingdilite.feature.news.generated.resources.article_fire_count
import yingdilite.feature.news.generated.resources.article_write_comment
import yingdilite.feature.news.generated.resources.news_comments

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleInteractionBar(
    replyNum: String,
    likeNum: String,
    enabled: Boolean,
    onCommentsClick: (() -> Unit)?,
    onWriteCommentClick: () -> Unit,
    onFireClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    scrollBehavior: BottomAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val replies = replyNum.ifEmpty { "0" }
    val likes = likeNum.ifEmpty { "0" }
    val writeCommentLabel = stringResource(Res.string.article_write_comment)
    BottomAppBar(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        windowInsets = WindowInsets(0, 0, 0, 0),
        scrollBehavior = scrollBehavior,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.3f
            val gutter = if (compact) 8.dp else 16.dp
            val gap = if (compact) 0.dp else 4.dp
            val actionWidth = ((maxWidth - gutter * 2 - 96.dp - gap * 3) / 2)
                .coerceIn(48.dp, 80.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = gutter),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = onWriteCommentClick,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = writeCommentLabel },
                    contentPadding = PaddingValues(
                        horizontal = if (compact) 8.dp else 12.dp,
                        vertical = 8.dp,
                    ),
                ) {
                    Icon(AppIcon.Edit.imageVector, null, Modifier.size(24.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = writeCommentLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clearAndSetSemantics {},
                    )
                }
                CountAction(
                    icon = AppIcon.Comment,
                    count = replies,
                    label = stringResource(Res.string.news_comments, replies),
                    maxWidth = actionWidth,
                    enabled = enabled,
                    onClick = onCommentsClick,
                )
                CountAction(
                    icon = AppIcon.Fire,
                    count = likes,
                    label = stringResource(Res.string.article_fire_count, likes),
                    maxWidth = actionWidth,
                    enabled = enabled,
                    onClick = onFireClick,
                    tint = colors.error,
                )
                IconButton(onClick = onFavoriteClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
                    Icon(AppIcon.Favorite.imageVector, stringResource(Res.string.article_favorite))
                }
            }
        }
    }
}

@Composable
private fun CountAction(
    icon: AppIcon,
    count: String,
    label: String,
    maxWidth: Dp,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val actionModifier = Modifier
        .widthIn(min = 48.dp, max = maxWidth)
        .heightIn(min = 48.dp)
        .semantics(mergeDescendants = true) { contentDescription = label }
    if (onClick != null) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = actionModifier,
            colors = ButtonDefaults.textButtonColors(contentColor = tint),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            CountActionContent(icon, count)
        }
    } else {
        Row(
            modifier = actionModifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountActionContent(icon, count, tint)
        }
    }
}

@Composable
private fun RowScope.CountActionContent(
    icon: AppIcon,
    count: String,
    tint: Color = LocalContentColor.current,
) {
    Icon(icon.imageVector, null, Modifier.size(24.dp), tint = tint)
    Spacer(Modifier.width(4.dp))
    Text(
        text = count,
        color = tint,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false).clearAndSetSemantics {},
    )
}
