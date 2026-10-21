package com.graveyard.core.designsystem.webview.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class WebViewInteraction {
    /** Keep platform scrolling, gestures and inertia. */
    Native,
    /** Coordinate the main document with Compose parents, including shared vertical inertia. */
    NestedScroll,
}

enum class WebNavigationDecision { Allow, Block, OpenExternal }

data class WebNavigationRequest(val url: String, val newWindow: Boolean = false)

internal fun String.isWebUrl(): Boolean =
    startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)

internal data class WebViewConfiguration(
    val templateBridge: Boolean,
    val javaScript: Boolean = true,
    val domStorage: Boolean = !templateBridge,
)

internal sealed interface WebLoadEvent {
    data class Started(val navigation: Long, val url: String?) : WebLoadEvent
    data class Finished(val navigation: Long, val url: String?, val back: Boolean, val forward: Boolean) : WebLoadEvent
    data class Failed(val navigation: Long, val message: String, val httpStatus: Int? = null) : WebLoadEvent
    data object ProcessTerminated : WebLoadEvent
}

internal interface WebViewPort {
    var onMessage: ((String) -> Unit)?
    var onLoad: ((WebLoadEvent) -> Unit)?
    var onNavigation: ((WebNavigationRequest) -> WebNavigationDecision)?
    fun loadHtml(html: String, baseUrl: String)
    fun loadUrl(url: String)
    fun evaluate(script: String)
    fun reload()
    fun goBack()
    fun goForward()
    fun stopLoading()
    fun detach()
    /** Finish native media dismissal before a pooled template can be reused. */
    suspend fun closeMediaPresentations() {}
    fun destroy()
}

@Composable
internal expect fun rememberWebViewFactory(): (WebViewConfiguration) -> WebViewPort

@Composable
internal expect fun NativeWebView(port: WebViewPort, bindingId: Long, interaction: WebViewInteraction, modifier: Modifier)
