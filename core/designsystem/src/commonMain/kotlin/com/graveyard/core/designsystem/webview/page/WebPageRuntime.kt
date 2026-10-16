package com.graveyard.core.designsystem.webview.page

import com.graveyard.core.designsystem.webview.platform.WebLoadEvent
import com.graveyard.core.designsystem.webview.platform.WebNavigationDecision
import com.graveyard.core.designsystem.webview.platform.WebNavigationRequest
import com.graveyard.core.designsystem.webview.platform.WebViewConfiguration
import com.graveyard.core.designsystem.webview.platform.WebViewPort
import com.graveyard.core.designsystem.webview.platform.isWebUrl
import com.graveyard.core.designsystem.webview.pool.PooledWebViewEntry
import com.graveyard.core.designsystem.webview.pool.WebViewReuseKey

internal val WebPageReuseKey = WebViewReuseKey("page", WebViewConfiguration(templateBridge = false))

internal class WebPageRuntime(override val port: WebViewPort) : PooledWebViewEntry {
    private var state: WebPageState? = null
    private var source: WebPageSource? = null
    private var navigation = 0L
    private var destroyed = false
    private var recreate: (() -> Unit)? = null
    override var broken = false
        private set

    fun bind(
        target: WebPageState,
        onNavigation: (WebNavigationRequest) -> WebNavigationDecision,
        onRecreate: () -> Unit,
    ) {
        check(state == null && target.owner == null) { "A WebPageState can only be mounted once" }
        state = target
        recreate = onRecreate
        target.owner = this
        target.currentUrl = null
        target.error = null
        target.isLoading = false
        target.canGoBack = false
        target.canGoForward = false
        target.command = { command ->
            if (broken) { if (command == WebPageCommand.Reload) onRecreate() }
            else when (command) {
                WebPageCommand.Reload -> { if (source is WebPageSource.Url && target.currentUrl == null) loadSource() else port.reload() }
                WebPageCommand.Back -> port.goBack()
                WebPageCommand.Forward -> port.goForward()
            }
        }
        port.onNavigation = onNavigation
        port.onLoad = ::receive
    }

    fun update(value: WebPageSource) {
        if (source == value || destroyed || state == null) return
        if (broken) { recreate?.invoke(); return }
        source = value
        loadSource()
    }

    private fun loadSource() {
        val target = state ?: return
        port.stopLoading()
        target.error = null
        target.isLoading = true
        when (val value = source) {
            is WebPageSource.Html -> port.loadHtml(value.html, value.baseUrl)
            is WebPageSource.Url -> if (value.url.isWebUrl()) port.loadUrl(value.url) else {
                target.isLoading = false
                target.error = WebPageError("Only HTTP(S) URLs are supported")
            }
            null -> Unit
        }
    }

    private fun receive(event: WebLoadEvent) {
        val target = state ?: return
        if (destroyed || broken) return
        when (event) {
            is WebLoadEvent.Started -> if (event.navigation >= navigation) {
                navigation = event.navigation
                target.currentUrl = event.url
                target.isLoading = true
                target.error = null
            }
            is WebLoadEvent.Finished -> if (event.navigation == navigation) {
                target.currentUrl = event.url
                target.isLoading = false
                target.canGoBack = event.back
                target.canGoForward = event.forward
            }
            is WebLoadEvent.Failed -> if (event.navigation == navigation) {
                target.isLoading = false
                target.error = WebPageError(event.message, event.httpStatus)
            }
            WebLoadEvent.ProcessTerminated -> {
                broken = true
                target.isLoading = false
                target.canGoBack = false
                target.canGoForward = false
                target.error = WebPageError("Web content process terminated", processTerminated = true)
            }
        }
    }

    override fun unbind() {
        state?.takeIf { it.owner === this }?.let {
            it.owner = null
            it.command = null
            it.isLoading = false
            it.canGoBack = false
            it.canGoForward = false
        }
        state = null
        recreate = null
        port.onLoad = null
        port.onNavigation = null
        port.stopLoading()
        port.detach()
    }

    override suspend fun recycle(): Boolean = false

    override fun destroy() {
        if (destroyed) return
        destroyed = true
        unbind()
        port.onMessage = null
        port.destroy()
    }
}
