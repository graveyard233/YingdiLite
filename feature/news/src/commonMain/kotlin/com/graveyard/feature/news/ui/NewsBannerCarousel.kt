package com.graveyard.feature.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.graveyard.core.model.news.NewsBanner
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.news_banner_page_status

@Composable
internal fun NewsBannerCarousel(banners: List<NewsBanner>) {
    val state = rememberPagerState(pageCount = { banners.size })
    val minimumHeight = (44f * LocalDensity.current.fontScale + 24f).dp
    LaunchedEffect(banners.size) {
        if (state.currentPage >= banners.size) {
            state.scrollToPage(banners.lastIndex.coerceAtLeast(0))
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val bannerHeight = maxOf(maxWidth / 2.4f, minimumHeight)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .clip(MaterialTheme.shapes.medium),
        ) {
            HorizontalPager(
                state = state,
                modifier = Modifier.fillMaxSize(),
                key = { index -> "${banners[index].adId}-$index" },
            ) { index ->
                val banner = banners[index]
                Box(Modifier.fillMaxSize()) {
                    NewsImage(banner.img, Modifier.fillMaxSize())
                    if (banner.title.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    ),
                                ),
                        )
                        Text(
                            text = banner.title,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(
                                    start = 12.dp,
                                    end = 12.dp,
                                    bottom = if (banners.size > 1) 16.dp else 12.dp,
                                ),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (banners.size > 1) {
                val pageStatus = stringResource(
                    Res.string.news_banner_page_status,
                    state.currentPage + 1,
                    banners.size,
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 8.dp)
                        .semantics { stateDescription = pageStatus },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val indicatorColor = MaterialTheme.colorScheme.primaryFixed
                    if (banners.size > 7) {
                        Text(
                            "${state.currentPage + 1}/${banners.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = indicatorColor,
                        )
                    } else {
                        repeat(banners.size) { dotIndex ->
                            Box(
                                modifier = Modifier
                                    .size(width = if (state.currentPage == dotIndex) 20.dp else 4.dp, height = 4.dp)
                                    .background(
                                        if (state.currentPage == dotIndex) indicatorColor
                                        else Color.White.copy(alpha = 0.5f),
                                        CircleShape,
                                    )
                                    .clearAndSetSemantics {},
                            )
                        }
                    }
                }
            }
        }
    }
}
