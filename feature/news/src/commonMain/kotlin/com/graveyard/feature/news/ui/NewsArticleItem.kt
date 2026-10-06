package com.graveyard.feature.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.graveyard.core.model.news.NewsArticle

@Composable
internal fun NewsArticleItem(article: NewsArticle, nowEpochSeconds: Long) {
    val labels = remember(article.tagJson) {
        articleTagLabels(article.tagJson).take(2).joinToString(" / ")
    }
    val time = relativeNewsTime(article.showTime, nowEpochSeconds)
    val replies = remember(article.replyNum) { commentCount(article.replyNum) }
    val metadata = remember(time, replies) {
        listOfNotNull(time, replies?.let { "$it 评论" }).joinToString(" · ")
    }
    val colors = MaterialTheme.colorScheme
    val largeFont = LocalDensity.current.fontScale > 1.3f
    val cardHeight = if (largeFont) 184.dp else 144.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(MaterialTheme.shapes.large)
            .background(colors.surfaceContainerLow)
            .semantics(mergeDescendants = true) {},
    ) {
        Layout(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            content = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (labels.isNotEmpty()) {
                        Text(
                            text = labels,
                            modifier = Modifier
                                .background(colors.primaryFixed, MaterialTheme.shapes.extraSmall)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onPrimaryFixedVariant,
                            maxLines = if (largeFont) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        maxLines = if (largeFont) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (metadata.isNotEmpty()) {
                        Text(
                            text = metadata,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                NewsImage(
                    url = article.cover,
                    modifier = Modifier.clip(MaterialTheme.shapes.small),
                )
            },
        ) { measurables, constraints ->
            // Size the image during measurement without subcomposing each article.
            val compactImage = largeFont ||
                (constraints.hasBoundedWidth && constraints.maxWidth.toDp() < 300.dp)
            val preferredImageSize = (if (compactImage) 72.dp else 96.dp).roundToPx()
            val imageSize = minOf(
                preferredImageSize,
                constraints.maxWidth,
                constraints.maxHeight,
            )
            val image = measurables[1].measure(Constraints.fixed(imageSize, imageSize))
            val gap = if (constraints.hasBoundedWidth) {
                minOf(12.dp.roundToPx(), (constraints.maxWidth - image.width).coerceAtLeast(0))
            } else {
                12.dp.roundToPx()
            }
            val textMaxWidth = if (constraints.hasBoundedWidth) {
                (constraints.maxWidth - image.width - gap).coerceAtLeast(0)
            } else {
                Constraints.Infinity
            }
            val text = measurables[0].measure(
                constraints.copy(
                    minWidth = if (constraints.hasBoundedWidth) textMaxWidth else 0,
                    maxWidth = textMaxWidth,
                    minHeight = 0,
                ),
            )
            val width = constraints.constrainWidth(text.width + gap + image.width)
            val height = constraints.constrainHeight(maxOf(text.height, image.height))
            layout(width, height) {
                text.placeRelative(0, 0)
                image.placeRelative(width - image.width, 0)
            }
        }
    }
}

@Composable
internal fun NewsArticlePlaceholder() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp)
            .clip(MaterialTheme.shapes.large)
            .background(colors.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
