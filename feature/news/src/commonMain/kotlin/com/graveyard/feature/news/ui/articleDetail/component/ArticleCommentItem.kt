package com.graveyard.feature.news.ui.articleDetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.model.news.ArticleComment
import com.graveyard.core.model.news.ArticleCommentThread
import com.graveyard.feature.news.ui.newsList.component.relativeNewsTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.article_comment_avatar
import yingdilite.feature.news.generated.resources.article_comment_collapse
import yingdilite.feature.news.generated.resources.article_comment_deleted
import yingdilite.feature.news.generated.resources.article_comment_dislike
import yingdilite.feature.news.generated.resources.article_comment_dislike_count
import yingdilite.feature.news.generated.resources.article_comment_expand
import yingdilite.feature.news.generated.resources.article_comment_level
import yingdilite.feature.news.generated.resources.article_comment_like
import yingdilite.feature.news.generated.resources.article_comment_like_count
import yingdilite.feature.news.generated.resources.article_comment_manager
import yingdilite.feature.news.generated.resources.article_comment_more
import yingdilite.feature.news.generated.resources.article_comment_reply
import yingdilite.feature.news.generated.resources.article_comment_reply_to
import yingdilite.feature.news.generated.resources.article_comment_view_all_replies
import yingdilite.feature.news.generated.resources.article_comment_view_image

private const val ReplyPreviewLimit = 3
private const val CollapsedCommentLines = 5

@Composable
internal fun ArticleCommentItem(
    thread: ArticleCommentThread,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    onLikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onDislikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onMoreClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onViewReplies: (commentId: Long) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val comment = thread.mainComment
    val replies = thread.replies.take(ReplyPreviewLimit)
    val replyCount = maxOf(comment.replyNum, thread.replies.size)
    val needShowReply = replyCount > 0
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = if (needShowReply) 8.dp else 0.dp),
        ) {
            CommentBody(comment, comment.id, thread.isManager, true, true,
                onImageClick, onLikeClicked, onDislikeClicked, onMoreClicked)
            if (needShowReply) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 10.dp),
                ) {
                    replies.forEachIndexed { index, reply ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                        ReplyPreview(reply, comment.id, onImageClick, onLikeClicked, onDislikeClicked, onMoreClicked)
                    }
                    if (replyCount > replies.size) {
                        TextButton(
                            onClick = { onViewReplies(comment.id) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.article_comment_view_all_replies, replyCount),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
internal fun ArticleThreadCommentItem(
    comment: ArticleComment,
    parentCommentId: Long,
    isManager: Boolean,
    isRoot: Boolean,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    onLikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onDislikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onMoreClicked: (commentId: Long, parentCommentId: Long) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp)) {
            CommentBody(comment, parentCommentId, isManager, isRoot, isRoot,
                onImageClick, onLikeClicked, onDislikeClicked, onMoreClicked)
        }
        if (!isRoot) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun CommentBody(
    comment: ArticleComment,
    parentCommentId: Long,
    isManager: Boolean,
    showLevel: Boolean,
    collapsible: Boolean,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    onLikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onDislikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onMoreClicked: (commentId: Long, parentCommentId: Long) -> Unit,
) {
    CommentAuthorHeader(comment, isManager, showLevel = showLevel, onMoreClicked = {
        onMoreClicked(comment.id, parentCommentId)
    })
    CommentContent(comment, Modifier.padding(top = 6.dp), collapsible = collapsible)
    if (comment.isVisible && !comment.authorDelete) {
        CommentImages(comment.imageUrls, onImageClick)
        CommentActions(comment, parentCommentId, onLikeClicked, onDislikeClicked)
    }
}

@Composable
private fun ReplyPreview(
    comment: ArticleComment,
    parentCommentId: Long,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    onLikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onDislikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onMoreClicked: (commentId: Long, parentCommentId: Long) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        ReplyContent(comment)
        if (comment.isVisible && !comment.authorDelete) {
            CommentImages(comment.imageUrls, onImageClick, compact = true)
        }
        ReplyFooter(comment, parentCommentId, onLikeClicked, onDislikeClicked, onMoreClicked)
    }
}

@Composable
private fun CommentAuthorHeader(
    comment: ArticleComment,
    isManager: Boolean,
    onMoreClicked: () -> Unit,
    showLevel: Boolean = true,
) {
    val author = comment.author
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CommentAvatar(comment, 40.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = author.username.ifBlank { "—" },
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showLevel && author.level > 0) {
                    Text(
                        text = stringResource(Res.string.article_comment_level, author.level),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                if (isManager) {
                    Text(
                        text = stringResource(Res.string.article_comment_manager),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
            CommentMetadataText(AnnotatedString(commentMetadata(comment)))
        }
        CommentIconButton(
            icon = AppIcon.MoreVert,
            label = stringResource(Res.string.article_comment_more),
            onClick = onMoreClicked,
        )
    }
}

@Composable
private fun CommentAvatar(comment: ArticleComment, size: Dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (comment.author.head.isNotBlank()) {
            AsyncImage(
                model = comment.author.head,
                contentDescription = stringResource(Res.string.article_comment_avatar, comment.author.username),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = comment.author.username.take(1).ifBlank { "?" },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun ReplyContent(comment: ArticleComment) {
    val deleted = comment.authorDelete || !comment.isVisible
    val avatarDescription = stringResource(Res.string.article_comment_avatar, comment.author.username)
    val deletedText = stringResource(Res.string.article_comment_deleted)
    val receiver = if (comment.receiverName.isNotBlank()) {
        stringResource(Res.string.article_comment_reply_to, comment.receiverName)
    } else ""
    val nicknameStyle = MaterialTheme.typography.labelLarge.toSpanStyle().copy(fontWeight = FontWeight.SemiBold)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val text = buildAnnotatedString {
        appendInlineContent("avatar", avatarDescription)
        withStyle(nicknameStyle) { append(comment.author.username.ifBlank { "—" }) }
        append(": ")
        if (deleted) {
            withStyle(SpanStyle(color = muted)) { append(deletedText) }
        } else {
            if (receiver.isNotBlank()) {
                withStyle(SpanStyle(color = primary)) { append(receiver) }
                if (comment.content.isNotBlank()) append(" ")
            }
            append(comment.content)
        }
    }
    val density = LocalDensity.current
    // Convert dp to sp so font scaling does not enlarge the inline avatar or its gap.
    val avatarPlaceholder = with(density) {
        Placeholder(width = 30.dp.toSp(), height = 24.dp.toSp(), placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter)
    }
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        inlineContent = mapOf("avatar" to InlineTextContent(avatarPlaceholder) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                CommentAvatar(comment, 24.dp)
            }
        }),
    )
}

@Composable
private fun CommentContent(comment: ArticleComment, modifier: Modifier = Modifier, collapsible: Boolean = true) {
    if (comment.authorDelete || !comment.isVisible) {
        Text(
            text = stringResource(Res.string.article_comment_deleted),
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val receiver = if (comment.receiverName.isNotBlank()) {
            stringResource(Res.string.article_comment_reply_to, comment.receiverName)
        } else ""
        val primary = MaterialTheme.colorScheme.primary
        val text = remember(receiver, comment.content, primary) {
            buildAnnotatedString {
                if (receiver.isNotBlank()) {
                    withStyle(SpanStyle(color = primary)) { append(receiver) }
                    if (comment.content.isNotBlank()) append(" ")
                }
                append(comment.content)
            }
        }
        if (text.isNotBlank()) {
            var expanded by rememberSaveable(comment.id, comment.content, comment.receiverName) { mutableStateOf(false) }
            var canExpand by remember(text) { mutableStateOf(false) }
            Column(modifier = modifier) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (!collapsible || expanded) Int.MAX_VALUE else CollapsedCommentLines,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { layout ->
                        // Expanded layouts must not erase the collapsed overflow state.
                        if (!expanded) canExpand = layout.hasVisualOverflow
                    },
                )
                if (collapsible && (expanded || canExpand)) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text(stringResource(if (expanded) Res.string.article_comment_collapse else Res.string.article_comment_expand))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun commentMetadata(comment: ArticleComment): String {
    val time = relativeNewsTime(comment.created, Clock.System.now().epochSeconds)
    val location = comment.ipHome.takeUnless {
        it.isBlank() || it.equals("未知地点", ignoreCase = true) || it.equals("unknown", ignoreCase = true)
    }
    return listOfNotNull(time, location).joinToString(" · ")
}

@Composable
private fun CommentMetadataText(metadata: AnnotatedString, modifier: Modifier = Modifier) {
    if (metadata.isBlank()) return
    Text(
        text = metadata,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ReplyFooter(
    comment: ArticleComment,
    parentCommentId: Long,
    onLikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onDislikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onMoreClicked: (commentId: Long, parentCommentId: Long) -> Unit,
) {
    val metadata = AnnotatedString(commentMetadata(comment))
    val deleted = comment.authorDelete || !comment.isVisible
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val countStyle = MaterialTheme.typography.labelMedium
    val metadataStyle = MaterialTheme.typography.bodySmall
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val countWidths = listOf(comment.likeNum, comment.hateNum).map { count ->
            val textWidth = if (count > 0) {
                with(density) { textMeasurer.measure(AnnotatedString(count.toString()), countStyle).size.width.toDp() }
            } else 0.dp
            maxOf(48.dp, 12.dp + 20.dp + (if (count > 0) 4.dp + textWidth else 0.dp))
        }
        val actionsWidth = minOf(maxWidth, if (deleted) 48.dp else 96.dp + countWidths[0] + countWidths[1])
        val metadataWidth = with(density) {
            textMeasurer.measure(metadata, metadataStyle, maxLines = 1).size.width.toDp()
        }
        if (metadataWidth + 8.dp + actionsWidth <= maxWidth) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CommentMetadataText(metadata, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                CommentActions(
                    comment, parentCommentId, onLikeClicked, onDislikeClicked,
                    Modifier.width(actionsWidth), onMoreClicked,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CommentMetadataText(metadata)
                CommentActions(comment, parentCommentId, onLikeClicked, onDislikeClicked, onMoreClicked = onMoreClicked)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommentImages(
    imageUrls: List<String>,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    compact: Boolean = false,
) {
    if (imageUrls.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        val spacing = if (compact) 6.dp else 8.dp
        val imageWidth = if (compact) {
            minOf(maxWidth, 72.dp)
        } else {
            minOf(104.dp, ((maxWidth - spacing * 2) / 3).coerceAtLeast(0.dp))
        }
        val imageHeight = if (compact) 56.dp else imageWidth
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            imageUrls.forEachIndexed { index, url ->
                val imageDescription = stringResource(Res.string.article_comment_view_image, index + 1)
                Box(
                    modifier = Modifier.size(imageWidth, imageHeight).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .semantics(mergeDescendants = true) { contentDescription = imageDescription }
                        .clickable { onImageClick(imageUrls, index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcon.Image.imageVector, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentActions(
    comment: ArticleComment,
    parentCommentId: Long,
    onLikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onDislikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onMoreClicked: ((commentId: Long, parentCommentId: Long) -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val fixedActionsWidth = if (onMoreClicked != null) 96.dp else 48.dp
        val maxVoteWidth = maxOf(48.dp, (maxWidth - fixedActionsWidth) / 2)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (comment.isVisible && !comment.authorDelete) {
                CommentIconButton(
                    icon = AppIcon.Reply,
                    label = stringResource(Res.string.article_comment_reply),
                    onClick = {
                        // TODO: Connect the reply action.
                    },
                    enabled = false,
                )
                Spacer(Modifier.weight(1f))
                CommentAction(
                    modifier = Modifier.widthIn(max = maxVoteWidth),
                    icon = AppIcon.Fire,
                    count = comment.likeNum,
                    label = stringResource(Res.string.article_comment_like),
                    countLabel = stringResource(Res.string.article_comment_like_count, comment.likeNum.toString()),
                    onClick = { onLikeClicked(comment.id, parentCommentId) },
                )
                CommentAction(
                    modifier = Modifier.widthIn(max = maxVoteWidth),
                    icon = AppIcon.ThumbDown,
                    count = comment.hateNum,
                    label = stringResource(Res.string.article_comment_dislike),
                    countLabel = stringResource(Res.string.article_comment_dislike_count, comment.hateNum.toString()),
                    onClick = { onDislikeClicked(comment.id, parentCommentId) },
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (onMoreClicked != null) {
                CommentIconButton(
                    icon = AppIcon.MoreVert,
                    label = stringResource(Res.string.article_comment_more),
                    onClick = { onMoreClicked(comment.id, parentCommentId) },
                )
            }
        }
    }
}

@Composable
private fun CommentAction(
    icon: AppIcon,
    count: Int,
    label: String,
    countLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CommentTooltip(label) {
        TextButton(
            onClick = onClick,
            modifier = modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)
                .semantics(mergeDescendants = true) { contentDescription = "$label, $countLabel" },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            contentPadding = PaddingValues(horizontal = 6.dp),
        ) {
            Icon(icon.imageVector, contentDescription = null, modifier = Modifier.size(20.dp))
            if (count > 0) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = count.toString(),
                    modifier = Modifier.clearAndSetSemantics {},
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CommentIconButton(
    icon: AppIcon,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    CommentTooltip(label) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        ) {
            Icon(icon.imageVector, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentTooltip(label: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        content()
    }
}
