package com.graveyard.feature.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.model.news.NewsArticle

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NewsArticleItem(article: NewsArticle, nowEpochSeconds: Long) {
    val labels = remember(article.tagJson) { articleTagLabels(article.tagJson) }
    val time = relativeNewsTime(article.showTime, nowEpochSeconds)
    val replies = remember(article.replyNum) { commentCount(article.replyNum) }
    val colors = MaterialTheme.colorScheme
    val largeFont = LocalDensity.current.fontScale > 1.3f

    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        BoxWithConstraints(Modifier.padding(16.dp)) {
            val imageSize = if (maxWidth < 300.dp || largeFont) 72.dp else 96.dp
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (labels.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            labels.take(2).forEach { label ->
                                Text(
                                    text = label,
                                    modifier = Modifier
                                        .background(colors.primaryFixed, MaterialTheme.shapes.extraSmall)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onPrimaryFixedVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = if (largeFont) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (time != null || replies != null) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            time?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            replies?.let {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = AppIcon.Comment.imageVector,
                                        contentDescription = "评论",
                                        modifier = Modifier.size(14.dp),
                                        tint = colors.onSurfaceVariant,
                                    )
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                NewsImage(
                    url = article.cover,
                    modifier = Modifier.size(imageSize).clip(MaterialTheme.shapes.small),
                )
            }
        }
    }
}

@Composable
internal fun NewsArticlePlaceholder() {
    val colors = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.large, color = colors.surfaceContainerLow) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Spacer(Modifier.width(64.dp).height(16.dp).background(colors.surfaceContainerHigh))
                Spacer(Modifier.height(12.dp))
                Spacer(Modifier.fillMaxWidth().height(16.dp).background(colors.surfaceContainerHigh))
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.fillMaxWidth(0.7f).height(16.dp).background(colors.surfaceContainerHigh))
                Spacer(Modifier.height(12.dp))
                Spacer(Modifier.width(80.dp).height(12.dp).background(colors.surfaceContainerHigh))
            }
            NewsImage("", Modifier.size(96.dp).clip(MaterialTheme.shapes.small))
        }
    }
}
