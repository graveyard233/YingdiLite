package com.graveyard.core.designsystem.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.graveyard.core.designsystem.icons.AppIcon
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.jetbrains.compose.resources.stringResource
import yingdilite.core.designsystem.generated.resources.Res
import yingdilite.core.designsystem.generated.resources.image_gallery_close
import yingdilite.core.designsystem.generated.resources.image_gallery_download
import yingdilite.core.designsystem.generated.resources.image_gallery_load_failed

/** Double tap cycles through these zoom steps, in order. */
private val DOUBLE_TAP_SCALES = listOf(2f, 4f)

/**
 * Translucent black behind every gallery control and every indicator drawn on the scrim.
 *
 * Fixed rather than themed because the gallery overlays arbitrary image content and must read the
 * same in light and dark themes.
 */
private val ChromeBackground = Color.Black.copy(alpha = 0.48f)

/** Content color on [ChromeBackground]. */
private val ChromeContent = Color.White

/**
 * Full-screen gallery for a set of article images.
 *
 * Presentation belongs to the caller's overlay scene, so this page only asks for [onClose];
 * [onDownload] is a placeholder until downloading is implemented.
 *
 * The chrome is immersive: tapping shows or hides it, double tapping hides it, and while it is
 * hidden the system bars hide with it. At the resting zoom level, a vertical start enables free
 * two-dimensional dragging to dismiss the image in either vertical direction.
 */
@Composable
fun ImageGalleryPage(
    urls: List<String>,
    initialIndex: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onDownload: (String) -> Unit = {},
) {
    if (urls.isEmpty()) return
    MediaSystemBars()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(urls.indices),
        pageCount = { urls.size },
    )
    var chromeVisible by rememberSaveable { mutableStateOf(true) }
    // A SideEffect keeps the very first frame in sync; a LaunchedEffect would flash the bars.
    MediaSystemBarsVisibility(hidden = !chromeVisible)
    val currentUrl by rememberUpdatedState(urls[pagerState.currentPage])
    val download by rememberUpdatedState(onDownload)
    val close by rememberUpdatedState(onClose)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissState = remember(scope, density) {
        ImageGalleryDismissState(scope, density, onClose = { close() })
    }
    DisposableEffect(dismissState, pagerState.currentPage) {
        onDispose { dismissState.reset() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged(dismissState::updateViewport)
            .guardImageGalleryInput(dismissState),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - dismissState.progress }
                .background(MaterialTheme.colorScheme.scrim),
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = dismissState.isIdle,
        ) { page ->
            ZoomableImage(
                url = urls[page],
                onTap = { chromeVisible = !chromeVisible },
                // Hiding regardless of the zoom direction keeps the chrome out of the way.
                onDoubleTap = { chromeVisible = false },
                dismissState = dismissState,
                canStartDismiss = { page == pagerState.currentPage && !pagerState.isScrollInProgress },
            )
        }
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .graphicsLayer { alpha = dismissState.chromeAlpha }
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GalleryChromeButton(
                    icon = AppIcon.Download,
                    contentDescription = stringResource(Res.string.image_gallery_download),
                    onClick = { if (dismissState.isIdle) download(currentUrl) },
                    enabled = dismissState.isIdle,
                )
                // Equal spacers center the position indicator in the row, not between the icons.
                Spacer(Modifier.weight(1f))
                // A single image needs no position indicator.
                if (urls.size > 1) {
                    Surface(shape = CircleShape, color = ChromeBackground) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${urls.size}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = ChromeContent,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                GalleryChromeButton(
                    icon = AppIcon.Close,
                    contentDescription = stringResource(Res.string.image_gallery_close),
                    onClick = dismissState::close,
                    enabled = dismissState.isIdle,
                )
            }
        }
    }
}

/** Circular icon button on [ChromeBackground], legible over any image. */
@Composable
private fun GalleryChromeButton(
    icon: AppIcon,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Surface(shape = CircleShape, color = ChromeBackground) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon.imageVector, contentDescription, tint = ChromeContent)
        }
    }
}

@Composable
private fun ZoomableImage(
    url: String,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    dismissState: ImageGalleryDismissState,
    canStartDismiss: () -> Boolean,
) {
    val zoomState = rememberZoomState()
    val tap by rememberUpdatedState(onTap)
    val doubleTap by rememberUpdatedState(onDoubleTap)
    val canStart by rememberUpdatedState(canStartDismiss)
    var failed by remember(url) { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .imageGalleryDismissGestures(zoomState, dismissState, canStart = { canStart() }),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = dismissState.offset.x
                    translationY = dismissState.offset.y
                    val scale = 1f - 0.15f * dismissState.progress
                    scaleX = scale
                    scaleY = scale
                    alpha = dismissState.imageAlpha
                },
            contentAlignment = Alignment.Center,
        ) image@{
            if (failed) {
                Text(
                    text = stringResource(Res.string.image_gallery_load_failed),
                    color = ChromeContent,
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@image
            }
            CircularProgressIndicator(color = ChromeContent)
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                // Zoomable needs the intrinsic size to clamp panning and to zoom around the tap point.
                onSuccess = { zoomState.setContentSize(it.painter.intrinsicSize) },
                onError = { failed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .zoomable(
                        zoomState = zoomState,
                        zoomEnabled = dismissState.isIdle,
                        onTap = { if (dismissState.isIdle) tap() },
                        onDoubleTap = { position ->
                            if (dismissState.isIdle) {
                                doubleTap()
                                val target = DOUBLE_TAP_SCALES.firstOrNull { zoomState.scale < it - 0.01f }
                                zoomState.changeScale(target ?: 1f, position)
                            }
                        },
                    ),
            )
        }
    }
}
