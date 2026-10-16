@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.graveyard.core.designsystem.webview.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.Velocity
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLErrorCancelled
import platform.UIKit.UIColor
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerDelegateProtocol
import platform.UIKit.UIPanGestureRecognizer
import platform.UIKit.UIScrollView
import platform.UIKit.UIScrollViewDelegateProtocol
import platform.UIKit.UIView
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationResponse
import platform.WebKit.WKNavigationResponsePolicy
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWindowFeatures
import platform.WebKit.javaScriptEnabled
import platform.darwin.NSObject
import kotlin.math.abs

@Composable
internal actual fun rememberWebViewFactory(): (WebViewConfiguration) -> WebViewPort {
    return remember { { configuration: WebViewConfiguration -> IosWebViewPort(configuration) } }
}

@Composable
internal actual fun NativeWebView(port: WebViewPort, bindingId: Long, interaction: WebViewInteraction, modifier: Modifier) {
    val view = (port as IosWebViewPort).view
    key(bindingId, interaction) {
        val dispatcher = remember { NestedScrollDispatcher() }
        val scope = rememberCoroutineScope()
        UIKitView(
            factory = { IosWebViewContainer(view, dispatcher, scope, interaction) },
            modifier = if (interaction == WebViewInteraction.NestedScroll) modifier.nestedScroll(
                connection = object : NestedScrollConnection {},
                dispatcher = dispatcher,
            ) else modifier,
            onRelease = { container ->
                container.dispose()
                if (view.superview == container) view.removeFromSuperview()
            },
        )
    }
}

private class IosWebViewContainer(
    private val content: WKWebView,
    dispatcher: NestedScrollDispatcher,
    scope: CoroutineScope,
    interaction: WebViewInteraction,
) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val scrollCoordinator = IosScrollCoordinator(content.scrollView, dispatcher, scope)

    init {
        content.removeFromSuperview()
        content.scrollView.alwaysBounceVertical = true
        if (interaction == WebViewInteraction.NestedScroll) {
            content.scrollView.alwaysBounceHorizontal = false
            content.scrollView.directionalLockEnabled = true
            content.scrollView.delegate = scrollCoordinator
            content.scrollView.panGestureRecognizer.delegate = scrollCoordinator
        }
        addSubview(content)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        content.setFrame(bounds)
    }

    fun dispose() {
        content.scrollView.delegate = null
        content.scrollView.panGestureRecognizer.delegate = null
    }
}

private class IosScrollCoordinator(
    private val scrollView: UIScrollView,
    private val dispatcher: NestedScrollDispatcher,
    private val scope: CoroutineScope,
) : NSObject(), UIScrollViewDelegateProtocol, UIGestureRecognizerDelegateProtocol {
    private var lastY = scrollView.contentOffset.useContents { y }
    private var lastTranslationY = 0.0
    private var tracking = false
    private var dragging = false

    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean {
        val pan = gestureRecognizer as? UIPanGestureRecognizer ?: return true
        // Give the surrounding horizontal pager horizontal drags.
        return pan.velocityInView(pan.view).useContents {
            if (x == 0.0 && y == 0.0) return true
            abs(y) >= abs(x)
        }
    }

    override fun scrollViewWillBeginDragging(scrollView: UIScrollView) {
        tracking = true
        dragging = true
        lastY = scrollView.contentOffset.useContents { y }
        lastTranslationY = scrollView.panGestureRecognizer.translationInView(scrollView).useContents { y }
    }

    override fun scrollViewDidScroll(scrollView: UIScrollView) {
        if (!tracking) {
            lastY = scrollView.contentOffset.useContents { y }
            return
        }

        val y = scrollView.contentOffset.useContents { this.y }
        val deltaY = lastY - y
        val translationY = scrollView.panGestureRecognizer.translationInView(scrollView).useContents { this.y }
        val fingerDelta = if (dragging) translationY - lastTranslationY else 0.0
        val top = scrollView.adjustedContentInset.useContents { -top }
        // Split the pan delta at the scroll boundary so the app bar receives the remainder
        // without waiting for a second drag after the document reaches its top.
        val consumed = deltaY.toFloat()
        val available = if (fingerDelta > 0.0 && y <= top + 0.5) {
            (fingerDelta - deltaY).coerceAtLeast(0.0).toFloat()
        } else {
            0f
        }
        dispatcher.dispatchPostScroll(
            consumed = Offset(0f, consumed),
            available = Offset(0f, available),
            source = if (dragging) NestedScrollSource.UserInput else NestedScrollSource.SideEffect,
        )
        lastY = y
        lastTranslationY = translationY
    }

    override fun scrollViewDidEndDragging(scrollView: UIScrollView, willDecelerate: Boolean) {
        dragging = false
        if (!willDecelerate) finishScroll()
    }

    override fun scrollViewDidEndDecelerating(scrollView: UIScrollView) {
        finishScroll()
    }

    override fun scrollViewDidEndScrollingAnimation(scrollView: UIScrollView) {
        finishScroll()
    }

    private fun finishScroll() {
        if (!tracking) return
        tracking = false
        dragging = false
        scope.launch {
            dispatcher.dispatchPostFling(Velocity.Zero, Velocity.Zero)
        }
    }
}

private class IosWebViewPort(private val configuration: WebViewConfiguration) : WebViewPort {
    override var onMessage: ((String) -> Unit)? = null
    override var onLoad: ((WebLoadEvent) -> Unit)? = null
    override var onNavigation: ((WebNavigationRequest) -> WebNavigationDecision)? = null
    private var destroyed = false
    private var initialAction = false
    private var navigationId = 0L
    private var activeNavigation: WKNavigation? = null
    private val navigations = mutableMapOf<WKNavigation, Long>()
    private val messages = object : NSObject(), WKScriptMessageHandlerProtocol {
        override fun userContentController(userContentController: WKUserContentController, didReceiveScriptMessage: WKScriptMessage) {
            if (!destroyed) (didReceiveScriptMessage.body as? String)?.let { onMessage?.invoke(it) }
        }
    }
    private val navigation = object : NSObject(), WKNavigationDelegateProtocol {
        override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
            if (!destroyed) onLoad?.invoke(WebLoadEvent.ProcessTerminated)
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
            if (destroyed || didStartProvisionalNavigation == null) return
            val id = navigations[didStartProvisionalNavigation] ?: (++navigationId).also {
                navigations[didStartProvisionalNavigation] = it
            }
            if (id < navigationId) return
            activeNavigation = didStartProvisionalNavigation
            onLoad?.invoke(WebLoadEvent.Started(id, webView.URL?.absoluteString))
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            if (!destroyed && didFinishNavigation != null && didFinishNavigation == activeNavigation) {
                onLoad?.invoke(WebLoadEvent.Finished(navigationId, webView.URL?.absoluteString, webView.canGoBack, webView.canGoForward))
            }
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
            failed(didFailProvisionalNavigation, withError)
        }

        @ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
            failed(didFailNavigation, withError)
        }

        override fun webView(webView: WKWebView, decidePolicyForNavigationAction: WKNavigationAction, decisionHandler: (WKNavigationActionPolicy) -> Unit) {
            val action = decidePolicyForNavigationAction
            val url = action.request.URL?.absoluteString.orEmpty()
            val subframe = action.targetFrame?.mainFrame == false
            val initial = initialAction && action.targetFrame?.mainFrame == true
            if (initial) initialAction = false
            val allow = !destroyed && (initial || subframe || action.targetFrame == null || (
                action.targetFrame != null && onNavigation?.invoke(WebNavigationRequest(url)) == WebNavigationDecision.Allow
            ))
            decisionHandler(if (allow) WKNavigationActionPolicy.WKNavigationActionPolicyAllow else WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
        }

        override fun webView(webView: WKWebView, decidePolicyForNavigationResponse: WKNavigationResponse, decisionHandler: (WKNavigationResponsePolicy) -> Unit) {
            val response = decidePolicyForNavigationResponse.response as? NSHTTPURLResponse
            if (!destroyed && decidePolicyForNavigationResponse.forMainFrame && response != null && response.statusCode >= 400) {
                onLoad?.invoke(WebLoadEvent.Failed(navigationId, "HTTP ${response.statusCode}", response.statusCode.toInt()))
                decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyCancel)
            } else decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow)
        }
    }
    private val ui = object : NSObject(), WKUIDelegateProtocol {
        override fun webView(webView: WKWebView, createWebViewWithConfiguration: WKWebViewConfiguration, forNavigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures): WKWebView? {
            if (!destroyed) {
                val url = forNavigationAction.request.URL?.absoluteString.orEmpty()
                if (onNavigation?.invoke(WebNavigationRequest(url, true)) == WebNavigationDecision.Allow) loadUrl(url)
            }
            return null
        }
    }
    val view = WKWebView(CGRectMake(0.0, 0.0, 0.0, 0.0), WKWebViewConfiguration().apply {
        preferences.javaScriptEnabled = configuration.javaScript
        if (configuration.templateBridge) userContentController.addScriptMessageHandler(messages, "appWeb")
    }).apply {
        opaque = false
        backgroundColor = UIColor.clearColor
        scrollView.backgroundColor = UIColor.clearColor
        scrollView.showsHorizontalScrollIndicator = !this@IosWebViewPort.configuration.templateBridge
        navigationDelegate = navigation
        UIDelegate = ui
    }

    private fun failed(native: WKNavigation?, error: NSError) {
        if (!destroyed && native != null && native == activeNavigation && error.code != NSURLErrorCancelled.toLong()) {
            onLoad?.invoke(WebLoadEvent.Failed(navigationId, error.localizedDescription))
        }
    }

    private fun begin(url: String?, action: () -> WKNavigation?) {
        if (destroyed) return
        initialAction = true
        val id = ++navigationId
        activeNavigation = null
        onLoad?.invoke(WebLoadEvent.Started(id, url))
        activeNavigation = action()?.also { navigations[it] = id }
    }

    override fun loadHtml(html: String, baseUrl: String) { begin(baseUrl) { view.loadHTMLString(html, NSURL(string = baseUrl)) } }
    override fun loadUrl(url: String) {
        val target = NSURL(string = url)
        if (target == null) { onLoad?.invoke(WebLoadEvent.Failed(navigationId, "Invalid URL")); return }
        begin(url) { view.loadRequest(NSURLRequest.requestWithURL(target)) }
    }
    override fun reload() { begin(view.URL?.absoluteString) { view.reload() } }
    override fun goBack() { if (view.canGoBack) begin(null) { view.goBack() } }
    override fun goForward() { if (view.canGoForward) begin(null) { view.goForward() } }
    override fun stopLoading() {
        if (!destroyed) { navigationId++; activeNavigation = null; initialAction = false; view.stopLoading() }
    }
    override fun evaluate(script: String) {
        if (destroyed) return
        view.evaluateJavaScript(script) { _, error ->
            if (!destroyed && error != null) onLoad?.invoke(WebLoadEvent.Failed(navigationId, error.localizedDescription))
        }
    }

    override fun detach() { view.removeFromSuperview() }

    override fun destroy() {
        if (destroyed) return
        destroyed = true
        onMessage = null
        onLoad = null
        onNavigation = null
        navigations.clear()
        view.stopLoading()
        view.navigationDelegate = null
        view.UIDelegate = null
        view.configuration.userContentController.removeScriptMessageHandlerForName("appWeb")
        detach()
    }
}
