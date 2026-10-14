package com.graveyard.feature.news.ui.newsList.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.graveyard.core.model.news.NewsArticle
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.news_comments

@Composable
internal fun NewsArticleItem(
    article: NewsArticle,
    nowEpochSeconds: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val label = article.tags.firstOrNull().orEmpty()
    val time = relativeNewsTime(article.showTime, nowEpochSeconds)
    val replies = commentCount(article.replyNum)
    val metadata = listOfNotNull(
        time,
        replies?.let { stringResource(Res.string.news_comments, it) },
    ).joinToString(" · ")
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints {
        val compact = maxWidth < 400.dp || LocalDensity.current.fontScale > 1.3f
        val largeText = LocalDensity.current.fontScale > 1.3f
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(if (selected) colors.secondaryContainer else colors.surfaceContainerLowest)
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) { this.selected = selected },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 112.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        maxLines = if (largeText) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (label.isNotEmpty()) {
                            Text(
                                text = label,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .background(colors.primaryFixed, MaterialTheme.shapes.extraSmall)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onPrimaryFixedVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (metadata.isNotEmpty()) {
                            Text(
                                text = metadata,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                NewsImage(
                    url = article.cover,
                    modifier = Modifier
                        .size(width = if (compact) 72.dp else 124.dp, height = if (compact) 72.dp else 88.dp)
                        .clip(MaterialTheme.shapes.small),
                )
            }
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f))
        }
    }
}

@Composable
internal fun NewsArticlePlaceholder() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(colors.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Spacer(Modifier.fillMaxWidth().height(16.dp).background(colors.surfaceContainerHigh))
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.fillMaxWidth(0.7f).height(16.dp).background(colors.surfaceContainerHigh))
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.width(64.dp).height(16.dp).background(colors.surfaceContainerHigh))
                    Spacer(Modifier.width(80.dp).height(12.dp).background(colors.surfaceContainerHigh))
                }
            }
            NewsImage("", Modifier.size(width = 124.dp, height = 88.dp).clip(MaterialTheme.shapes.small))
        }
    }
}
