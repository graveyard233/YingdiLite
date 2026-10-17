package com.graveyard.core.designsystem.webview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
        RichContentTheme(it.surface.css(), it.onSurface.css(), it.primary.css(), it.surface.luminance() < 0.5f)
    },
    interaction: WebViewInteraction = WebViewInteraction.Native,
    onNavigation: (WebNavigationRequest) -> WebNavigationDecision = { WebNavigationDecision.Block },
    onEvent: (RichContentEvent) -> Unit = {},
    templateData: JsonObject = JsonObject(emptyMap()),
) {
    val pool = LocalWebViewPool.current
    val callback by rememberUpdatedState(onEvent)
    val navigation by rememberUpdatedState(onNavigation)
    val uriHandler = LocalUriHandler.current
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
    Box(modifier) {
        if (!failed) lease?.let { NativeWebView(it.entry.port, it.id, interaction, Modifier.fillMaxSize()) }
        if (failed) Button(
            onClick = { failed = false; attempt = 0 }, modifier = Modifier.align(Alignment.Center),
        ) { Text(stringResource(Res.string.rich_content_retry)) }
        else if (!loaded) CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}

private fun Color.css(): String = "#" + (toArgb() and 0x00ffffff).toString(16).padStart(6, '0')
