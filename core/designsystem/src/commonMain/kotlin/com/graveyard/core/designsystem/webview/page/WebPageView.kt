package com.graveyard.core.designsystem.webview.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalUriHandler
import com.graveyard.core.designsystem.webview.LocalWebViewPool
import com.graveyard.core.designsystem.webview.platform.NativeWebView
import com.graveyard.core.designsystem.webview.platform.WebNavigationDecision
import com.graveyard.core.designsystem.webview.platform.WebNavigationRequest
import com.graveyard.core.designsystem.webview.platform.WebViewInteraction
import com.graveyard.core.designsystem.webview.platform.isWebUrl
import com.graveyard.core.designsystem.webview.pool.WebViewPool
import org.jetbrains.compose.resources.stringResource
import yingdilite.core.designsystem.generated.resources.Res
import yingdilite.core.designsystem.generated.resources.rich_content_retry

@Composable
fun WebPageView(
    source: WebPageSource,
    modifier: Modifier = Modifier,
    state: WebPageState = rememberWebPageState(),
    interaction: WebViewInteraction = WebViewInteraction.Native,
    onNavigation: (WebNavigationRequest) -> WebNavigationDecision = {
        if (it.url.isWebUrl()) WebNavigationDecision.Allow else WebNavigationDecision.Block
    },
) {
    val pool = LocalWebViewPool.current
    val navigation by rememberUpdatedState(onNavigation)
    val uriHandler = LocalUriHandler.current
    var attempt by remember(state) { mutableStateOf(0) }
    var lease by remember(pool, state, attempt) { mutableStateOf<WebViewPool.Lease?>(null) }
    DisposableEffect(pool, state, attempt) {
        val acquired = pool.acquire(WebPageReuseKey) { port, _ -> WebPageRuntime(port) }
        lease = acquired
        (acquired.entry as WebPageRuntime).bind(state, { request ->
            navigation(request).also { decision ->
                if (decision == WebNavigationDecision.OpenExternal) runCatching { uriHandler.openUri(request.url) }
            }
        }, { attempt++ })
        onDispose { pool.release(acquired) }
    }
    SideEffect { (lease?.entry as? WebPageRuntime)?.update(source) }
    Box(modifier) {
        lease?.let { NativeWebView(it.entry.port, it.id, interaction, Modifier.fillMaxSize()) }
        if (state.error != null) Button(onClick = state::reload, modifier = Modifier.align(Alignment.Center)) {
            Text(stringResource(Res.string.rich_content_retry))
        } else if (state.isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}
