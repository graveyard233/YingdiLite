package com.graveyard.feature.news.ui.articleDetail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.graveyard.core.data.repository.ArticleVote
import com.graveyard.core.designsystem.icons.AppIcon
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_favorite
import yingdilite.feature.news.generated.resources.article_fire_count
import yingdilite.feature.news.generated.resources.article_hate
import yingdilite.feature.news.generated.resources.article_comments_write_short
import yingdilite.feature.news.generated.resources.article_to_top
import yingdilite.feature.news.generated.resources.news_comments

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ArticleInteractionBar(
    replyNum: String,
    likeNum: String,
    vote: ArticleVote,
    collected: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    voteEnabled: Boolean,
    isVoteCoolingDown: Boolean,
    voteCooldownProgress: () -> Float,
    onCommentsClick: (() -> Unit)?,
    onWriteCommentClick: () -> Unit,
    onFireClick: () -> Unit,
    onHateClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onToTopClick: () -> Unit,
    scrollBehavior: BottomAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val hateTint = LocalContentColor.current
    val replies = replyNum.ifEmpty { "0" }
    val likes = likeNum.ifEmpty { "0" }
    val liked = vote == ArticleVote.Like
    val hated = vote == ArticleVote.Hate
    FlexibleBottomAppBar(
        modifier = modifier,
        // Set the expanded height without constraining the collapsing layout.
        expandedHeight = 56.dp,
        horizontalArrangement = Arrangement.Start,
        contentPadding = PaddingValues(0.dp),
        windowInsets = WindowInsets(0, 0, 0, 0),
        scrollBehavior = scrollBehavior,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.3f
            val gutter = if (compact) 8.dp else 16.dp
            val gap = if (compact) 0.dp else 4.dp
            // Four fixed 48dp buttons (to top, write, hate, favorite) plus the gaps between actions.
            val actionWidth = ((maxWidth - gutter * 2 - 192.dp - gap * 4 - 8.dp) / 2)
                .coerceIn(48.dp, 80.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onToTopClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
                        Icon(AppIcon.ToTop.imageVector, stringResource(Res.string.article_to_top))
                    }
                    IconButton(onClick = onWriteCommentClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
                        Icon(AppIcon.Edit.imageVector, stringResource(Res.string.article_comments_write_short))
                    }
                }
                Spacer(Modifier.weight(1f).widthIn(min = 8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CountAction(
                        icon = AppIcon.Comment,
                        count = replies,
                        label = stringResource(Res.string.news_comments, replies),
                        maxWidth = actionWidth,
                        enabled = enabled,
                        onClick = onCommentsClick,
                    )
                    CountAction(
                        icon = if (liked) AppIcon.FireFill else AppIcon.Fire,
                        count = likes,
                        label = stringResource(Res.string.article_fire_count, likes),
                        maxWidth = actionWidth,
                        enabled = enabled && interactionEnabled && voteEnabled,
                        onClick = onFireClick,
                        tint = if (liked) colors.error else LocalContentColor.current,
                        selected = liked,
                        voteCoolingDown = isVoteCoolingDown,
                        cooldownProgress = voteCooldownProgress,
                    )
                    IconButton(
                        onClick = onHateClick,
                        enabled = enabled && interactionEnabled && voteEnabled,
                        modifier = Modifier.size(48.dp).semantics { selected = hated },
                    ) {
                        VoteCooldownIcon(
                            icon = if (hated) AppIcon.ThumbDownFill else AppIcon.ThumbDown,
                            isCoolingDown = isVoteCoolingDown,
                            progress = voteCooldownProgress,
                            activeTint = hateTint,
                            disabledTint = hateTint.copy(alpha = hateTint.alpha * 0.38f),
                            contentDescription = stringResource(Res.string.article_hate),
                        )
                    }
                    IconButton(
                        onClick = onFavoriteClick,
                        enabled = enabled && interactionEnabled,
                        modifier = Modifier.size(48.dp).semantics { selected = collected },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (collected) colors.primary else LocalContentColor.current,
                        ),
                    ) {
                        Icon(
                            imageVector = (if (collected) AppIcon.FavoriteFill else AppIcon.Favorite).imageVector,
                            contentDescription = stringResource(Res.string.article_favorite),
                        )
                    }
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
    tint: Color = LocalContentColor.current,
    /** Non-null for toggle actions so accessibility services announce the current state. */
    selected: Boolean? = null,
    voteCoolingDown: Boolean = false,
    cooldownProgress: () -> Float = { 1f },
) {
    val iconColors = IconButtonDefaults.iconButtonColors(contentColor = tint)
    val actionModifier = Modifier
        .widthIn(min = 48.dp, max = maxWidth)
        .heightIn(min = 48.dp)
        .semantics(mergeDescendants = true) {
            contentDescription = label
            if (selected != null) this.selected = selected
        }
    if (onClick != null) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = actionModifier,
            colors = ButtonDefaults.textButtonColors(
                contentColor = iconColors.contentColor,
                disabledContentColor = iconColors.disabledContentColor,
            ),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            CountActionContent(
                icon = icon,
                count = count,
                tint = iconColors.contentColor,
                voteCoolingDown = voteCoolingDown,
                cooldownProgress = cooldownProgress,
                disabledTint = iconColors.disabledContentColor,
            )
        }
    } else {
        Row(
            modifier = actionModifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountActionContent(
                icon = icon,
                count = count,
                tint = if (enabled) iconColors.contentColor else iconColors.disabledContentColor,
                countTint = if (enabled) iconColors.contentColor else iconColors.disabledContentColor,
            )
        }
    }
}

@Composable
internal fun RowScope.CountActionContent(
    icon: AppIcon,
    count: String,
    tint: Color = LocalContentColor.current,
    countTint: Color = LocalContentColor.current,
    voteCoolingDown: Boolean = false,
    cooldownProgress: () -> Float = { 1f },
    activeTint: Color = tint,
    disabledTint: Color = activeTint.copy(alpha = activeTint.alpha * 0.38f),
) {
    if (!voteCoolingDown) {
        CountActionLabel(icon = icon, count = count, tint = tint, countTint = countTint)
        return
    }
    Box(Modifier.weight(1f, fill = false)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CountActionLabel(icon = icon, count = count, tint = disabledTint, countTint = disabledTint)
        }
        Row(
            modifier = Modifier
                .matchParentSize()
                .clearAndSetSemantics {}
                .voteCooldownReveal(cooldownProgress),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountActionLabel(icon = icon, count = count, tint = activeTint, countTint = activeTint)
        }
    }
}

@Composable
private fun RowScope.CountActionLabel(
    icon: AppIcon,
    count: String,
    tint: Color,
    countTint: Color,
) {
    Icon(icon.imageVector, null, Modifier.size(24.dp), tint = tint)
    Spacer(Modifier.width(4.dp))
    Text(
        text = count,
        color = countTint,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false).clearAndSetSemantics {},
    )
}

@Composable
internal fun VoteCooldownIcon(
    icon: AppIcon,
    isCoolingDown: Boolean,
    progress: () -> Float,
    activeTint: Color,
    disabledTint: Color,
    contentDescription: String? = null,
    modifier: Modifier = Modifier.size(24.dp),
) {
    if (!isCoolingDown) {
        Icon(icon.imageVector, contentDescription, modifier)
        return
    }
    Box(modifier) {
        Icon(icon.imageVector, contentDescription, Modifier.fillMaxSize(), tint = disabledTint)
        Icon(
            imageVector = icon.imageVector,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().voteCooldownReveal(progress),
            tint = activeTint,
        )
    }
}

// Read progress only during drawing so animation frames do not recompose or measure the content.
private fun Modifier.voteCooldownReveal(progress: () -> Float): Modifier = drawWithContent {
    val visibleFraction = progress().coerceIn(0f, 1f)
    if (visibleFraction > 0f) {
        clipRect(bottom = size.height * visibleFraction) {
            this@drawWithContent.drawContent()
        }
    }
}
