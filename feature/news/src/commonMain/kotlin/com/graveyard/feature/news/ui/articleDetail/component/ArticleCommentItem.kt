package com.graveyard.feature.news.ui.articleDetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import yingdilite.feature.news.generated.resources.article_comment_deleted
import yingdilite.feature.news.generated.resources.article_comment_dislike
import yingdilite.feature.news.generated.resources.article_comment_dislike_count
import yingdilite.feature.news.generated.resources.article_comment_level
import yingdilite.feature.news.generated.resources.article_comment_like
import yingdilite.feature.news.generated.resources.article_comment_like_count
import yingdilite.feature.news.generated.resources.article_comment_manager
import yingdilite.feature.news.generated.resources.article_comment_more
import yingdilite.feature.news.generated.resources.article_comment_more_replies
import yingdilite.feature.news.generated.resources.article_comment_reply
import yingdilite.feature.news.generated.resources.article_comment_reply_to
import yingdilite.feature.news.generated.resources.article_comment_view_image

@Composable
internal fun ArticleCommentCard(
    thread: ArticleCommentThread,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
    onLikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onDislikeClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    onMoreClicked: (commentId: Long, parentCommentId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val comment = thread.mainComment
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CommentAuthorHeader(comment = comment, isManager = thread.isManager, compact = false)
            CommentContent(comment)
            if (comment.isVisible && !comment.authorDelete) CommentImages(comment.imageUrls, onImageClick)
            CommentActions(comment, comment.id, onLikeClicked, onDislikeClicked, onMoreClicked)
            if (thread.replies.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    thread.replies.forEachIndexed { index, reply ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ReplyPreview(reply, comment.id, onImageClick, onLikeClicked, onDislikeClicked, onMoreClicked)
                    }
                }
            }
            val remainingReplies = (comment.replyNum - thread.replies.size).coerceAtLeast(0)
            if (remainingReplies > 0) {
                TextButton(onClick = {
                    // TODO: Load the remaining replies.
                }, enabled = false) {
                    Text(stringResource(Res.string.article_comment_more_replies, remainingReplies))
                }
            }
        }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CommentAuthorHeader(comment = comment, isManager = false, compact = true)
        CommentContent(comment)
        if (comment.isVisible && !comment.authorDelete) CommentImages(comment.imageUrls, onImageClick)
        CommentActions(comment, parentCommentId, onLikeClicked, onDislikeClicked, onMoreClicked)
    }
}

@OptIn(ExperimentalTime::class, ExperimentalLayoutApi::class)
@Composable
private fun CommentAuthorHeader(comment: ArticleComment, isManager: Boolean, compact: Boolean) {
    val author = comment.author
    val time = relativeNewsTime(comment.created, Clock.System.now().epochSeconds)
    val location = comment.ipHome.takeUnless {
        it.isBlank() || it.equals("未知地点", ignoreCase = true) || it.equals("unknown", ignoreCase = true)
    }
    val avatarSize = if (compact) 32.dp else 40.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(avatarSize).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (author.head.isNotBlank()) {
                AsyncImage(
                    model = author.head,
                    contentDescription = stringResource(Res.string.article_comment_avatar, author.username),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(avatarSize).clip(CircleShape),
                )
            } else {
                Text(
                    text = author.username.take(1).ifBlank { "?" },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = author.username.ifBlank { "—" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (author.level > 0) {
                    Text(
                        text = stringResource(Res.string.article_comment_level, author.level),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
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
            val metadata = listOfNotNull(time, location).joinToString(" · ")
            if (metadata.isNotBlank()) {
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CommentContent(comment: ArticleComment) {
    if (comment.authorDelete || !comment.isVisible) {
        Text(
            text = stringResource(Res.string.article_comment_deleted),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        if (comment.receiverName.isNotBlank()) {
            Text(
                text = stringResource(Res.string.article_comment_reply_to, comment.receiverName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (comment.content.isNotBlank()) {
            Text(comment.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommentImages(
    imageUrls: List<String>,
    onImageClick: (urls: List<String>, index: Int) -> Unit,
) {
    if (imageUrls.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val spacing = 8.dp
        val imageSize = minOf(104.dp, (maxWidth - spacing * 2) / 3)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            imageUrls.forEachIndexed { index, url ->
                val imageDescription = stringResource(Res.string.article_comment_view_image, index + 1)
                Box(
                    modifier = Modifier.size(imageSize).clip(MaterialTheme.shapes.medium)
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
                        modifier = Modifier.size(imageSize).clip(MaterialTheme.shapes.medium),
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
    onMoreClicked: (commentId: Long, parentCommentId: Long) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            // TODO: Connect the reply action.
        }, enabled = false, modifier = Modifier.size(48.dp)) {
            Icon(AppIcon.Comment.imageVector, stringResource(Res.string.article_comment_reply))
        }
        CommentAction(
            modifier = Modifier.weight(1f),
            icon = AppIcon.Fire,
            count = comment.likeNum,
            label = stringResource(Res.string.article_comment_like),
            countLabel = stringResource(Res.string.article_comment_like_count, comment.likeNum.toString()),
            onClick = { onLikeClicked(comment.id, parentCommentId) },
        )
        CommentAction(
            modifier = Modifier.weight(1f),
            icon = AppIcon.ThumbDown,
            count = comment.hateNum,
            label = stringResource(Res.string.article_comment_dislike),
            countLabel = stringResource(Res.string.article_comment_dislike_count, comment.hateNum.toString()),
            onClick = { onDislikeClicked(comment.id, parentCommentId) },
        )
        IconButton(onClick = { onMoreClicked(comment.id, parentCommentId) }, modifier = Modifier.size(48.dp)) {
            Icon(AppIcon.MoreVert.imageVector, stringResource(Res.string.article_comment_more), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Row(
        modifier = modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label, $countLabel" }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon.imageVector, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = count.toString(),
            modifier = Modifier.weight(1f).clearAndSetSemantics {},
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
