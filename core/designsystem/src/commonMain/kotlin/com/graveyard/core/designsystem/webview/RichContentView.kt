package com.graveyard.core.designsystem.webview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.graveyard.core.designsystem.webview.platform.NativeWebView
import com.graveyard.core.designsystem.webview.platform.WebNavigationDecision
import com.graveyard.core.designsystem.webview.platform.WebNavigationRequest
import com.graveyard.core.designsystem.webview.platform.WebViewInteraction
import com.graveyard.core.designsystem.webview.pool.WebViewPool
import com.graveyard.core.designsystem.webview.template.RichContentDocument
import com.graveyard.core.designsystem.webview.template.RichContentTemplate
import com.graveyard.core.designsystem.webview.template.TemplateRuntime
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import yingdilite.core.designsystem.generated.resources.Res
import yingdilite.core.designsystem.generated.resources.rich_content_retry

@OptIn(ExperimentalResourceApi::class)
@Composable
/** Local templates keep their shell. Handle links in the caller or return OpenExternal. */
fun RichContentView(
    template: RichContentTemplate,
    contentKey: String,
    html: String,
    baseUrl: String,
    readingState: RichContentReadingState,
    modifier: Modifier = Modifier,
    theme: RichContentTheme = MaterialTheme.colorScheme.let {
        RichContentTheme(
            background = it.surface.css(),
            foreground = it.onSurface.css(),
            link = it.primary.css(),
            dark = it.surface.luminance() < 0.5f,
            roles = it.toRoleColors(),
        )
    },
    interaction: WebViewInteraction = WebViewInteraction.Native,
    onNavigation: (WebNavigationRequest) -> WebNavigationDecision = { WebNavigationDecision.Block },
    onEvent: (RichContentEvent) -> Unit = {},
    templateData: JsonObject = JsonObject(emptyMap()),
    isVisible: Boolean = true,
) {
    val pool = LocalWebViewPool.current
    val callback by rememberUpdatedState(onEvent)
    val navigation by rememberUpdatedState(onNavigation)
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val key = template.reuseKey
    var attempt by remember(key, contentKey) { mutableStateOf(0) }
    var failed by remember(key, contentKey) { mutableStateOf(false) }
    var loaded by remember(key, contentKey, html, baseUrl, templateData, attempt) { mutableStateOf(false) }
    var lease by remember(pool, key, contentKey, readingState, attempt) { mutableStateOf<WebViewPool.Lease?>(null) }
    val handler by rememberUpdatedState<(RichContentEvent) -> Unit> { event ->
        when (event) {
            RichContentEvent.Rendered -> loaded = true
            RichContentEvent.Error -> if (attempt == 0) attempt++ else failed = true
            is RichContentEvent.LinkClick -> if (navigation(WebNavigationRequest(event.url)) == WebNavigationDecision.OpenExternal) {
                runCatching { uriHandler.openUri(event.url) }
            }
            else -> Unit
        }
        callback(event)
    }
    DisposableEffect(pool, key, contentKey, readingState, attempt, failed) {
        val acquired = if (!failed) pool.acquire(key) { port, instance ->
            TemplateRuntime(port, instance, template, pool.scope) { Res.readBytes("files/template_bridge.js").decodeToString() }
        } else null
        lease = acquired
        (acquired?.entry as? TemplateRuntime)?.bind(acquired.id, readingState) { handler(it) }
        onDispose { acquired?.let(pool::release) }
    }
    SideEffect {
        (lease?.entry as? TemplateRuntime)?.update(
            RichContentDocument(contentKey, html, baseUrl, templateData),
            theme,
        )
    }
    DisposableEffect(lease, lifecycleOwner, isVisible) {
        val runtime = lease?.entry as? TemplateRuntime
        if (!isVisible) runtime?.captureReadingPosition()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) runtime?.captureReadingPosition()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Box(modifier) {
        if (!failed) lease?.let { NativeWebView(it.entry.port, it.id, interaction, Modifier.fillMaxSize()) }
        if (failed) Button(
            onClick = { failed = false; attempt = 0 }, modifier = Modifier.align(Alignment.Center),
        ) { Text(stringResource(Res.string.rich_content_retry)) }
        else if (!loaded) CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}

private fun Color.css(): String = "#" + (toArgb() and 0x00ffffff).toString(16).padStart(6, '0')

/**
 * Every Material role, keyed by `ColorScheme` property name. Templates use the exact palette
 * instead of approximating containers with `color-mix`; keys are plain camelCase so the shell can
 * turn them into `--md-sys-color-*` custom properties without a shared name table.
 */
private fun ColorScheme.toRoleColors(): Map<String, String> = buildMap(48) {
    fun role(name: String, color: Color) { put(name, color.css()) }
    role("primary", primary)
    role("onPrimary", onPrimary)
    role("primaryContainer", primaryContainer)
    role("onPrimaryContainer", onPrimaryContainer)
    role("inversePrimary", inversePrimary)
    role("secondary", secondary)
    role("onSecondary", onSecondary)
    role("secondaryContainer", secondaryContainer)
    role("onSecondaryContainer", onSecondaryContainer)
    role("tertiary", tertiary)
    role("onTertiary", onTertiary)
    role("tertiaryContainer", tertiaryContainer)
    role("onTertiaryContainer", onTertiaryContainer)
    role("background", background)
    role("onBackground", onBackground)
    role("surface", surface)
    role("onSurface", onSurface)
    role("surfaceVariant", surfaceVariant)
    role("onSurfaceVariant", onSurfaceVariant)
    role("surfaceTint", surfaceTint)
    role("inverseSurface", inverseSurface)
    role("inverseOnSurface", inverseOnSurface)
    role("error", error)
    role("onError", onError)
    role("errorContainer", errorContainer)
    role("onErrorContainer", onErrorContainer)
    role("outline", outline)
    role("outlineVariant", outlineVariant)
    role("scrim", scrim)
    role("surfaceDim", surfaceDim)
    role("surfaceBright", surfaceBright)
    role("surfaceContainerLowest", surfaceContainerLowest)
    role("surfaceContainerLow", surfaceContainerLow)
    role("surfaceContainer", surfaceContainer)
    role("surfaceContainerHigh", surfaceContainerHigh)
    role("surfaceContainerHighest", surfaceContainerHighest)
    role("primaryFixed", primaryFixed)
    role("primaryFixedDim", primaryFixedDim)
    role("onPrimaryFixed", onPrimaryFixed)
    role("onPrimaryFixedVariant", onPrimaryFixedVariant)
    role("secondaryFixed", secondaryFixed)
    role("secondaryFixedDim", secondaryFixedDim)
    role("onSecondaryFixed", onSecondaryFixed)
    role("onSecondaryFixedVariant", onSecondaryFixedVariant)
    role("tertiaryFixed", tertiaryFixed)
    role("tertiaryFixedDim", tertiaryFixedDim)
    role("onTertiaryFixed", onTertiaryFixed)
    role("onTertiaryFixedVariant", onTertiaryFixedVariant)
}
