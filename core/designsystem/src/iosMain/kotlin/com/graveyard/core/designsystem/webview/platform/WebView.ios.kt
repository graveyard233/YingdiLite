@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.graveyard.core.designsystem.webview.platform

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.platform.LocalDensity
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.pointed
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIColor
import platform.UIKit.UIEvent
import platform.UIKit.UIEventTypeTouches
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateCancelled
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIGestureRecognizerStateFailed
import platform.UIKit.UIPanGestureRecognizer
import platform.UIKit.UIScrollView
import platform.UIKit.UIScrollViewDelegateProtocol
import platform.UIKit.UITouch
import platform.UIKit.UITouchPhase
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
import kotlin.coroutines.resume
import kotlin.math.abs

@Composable
internal actual fun rememberWebViewFactory(): (WebViewConfiguration) -> WebViewPort {
    return remember { { configuration: WebViewConfiguration -> IosWebViewPort(configuration) } }
}

@Composable
internal actual fun NativeWebView(port: WebViewPort, bindingId: Long, interaction: WebViewInteraction, modifier: Modifier) {
    val iosPort = port as IosWebViewPort
    val view = iosPort.view
    val density = LocalDensity.current.density
    key(bindingId, interaction, density) {
        val dispatcher = remember { NestedScrollDispatcher() }
        val connection = remember { object : NestedScrollConnection {} }
        val scope = rememberCoroutineScope()
        val decay = rememberSplineBasedDecay<Float>()
        val coordinator = remember(dispatcher, scope, decay) { WebViewScrollCoordinator(dispatcher, scope, decay) }
        UIKitView(
            factory = {
                iosPort.attach()
                IosWebViewContainer(view, coordinator, density, interaction)
            },
            modifier = if (interaction == WebViewInteraction.NestedScroll) modifier.nestedScroll(
                connection = connection,
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
    private val coordinator: WebViewScrollCoordinator,
    density: Float,
    interaction: WebViewInteraction,
) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val scrollCoordinator: IosScrollCoordinator?

    init {
        (content.superview as? IosWebViewContainer)?.dispose()
        content.removeFromSuperview()
        scrollCoordinator = if (interaction == WebViewInteraction.NestedScroll) {
            IosScrollCoordinator(content.scrollView, coordinator, density)
        } else null
        addSubview(content)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        val hit = super.hitTest(point, withEvent)
        // UIKit performs hit testing on touch-down, before the pan passes its recognition slop.
        if (hit != null && withEvent?.type == UIEventTypeTouches) scrollCoordinator?.touchDown(withEvent)
        return hit
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        if (content.superview === this) content.setFrame(bounds)
    }

    fun dispose() {
        coordinator.cancel()
        scrollCoordinator?.dispose()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        if (window == null) {
            coordinator.cancel()
            scrollCoordinator?.cancelGesture()
        }
    }

    override fun willRemoveSubview(subview: UIView) {
        // WebKit temporarily reparents this view for element fullscreen, then returns it here.
        // Keep the scroll delegate alive until UIKitView's onRelease disposes the container.
        if (subview === content) {
            coordinator.cancel()
            scrollCoordinator?.cancelGesture()
        }
        super.willRemoveSubview(subview)
    }
}

private class IosScrollCoordinator(
    private val scrollView: UIScrollView,
    private val coordinator: WebViewScrollCoordinator,
    private val density: Float,
) : NSObject(), UIScrollViewDelegateProtocol {
    private enum class DragAxis { Pending, Horizontal, Vertical }

    private val previousDelegate = scrollView.delegate
    private val previousBounceVertical = scrollView.alwaysBounceVertical
    private val previousBounceHorizontal = scrollView.alwaysBounceHorizontal
    private val previousDirectionalLock = scrollView.directionalLockEnabled
    private val panSelector = NSSelectorFromString("panChanged:")
    private var axis = DragAxis.Pending
    private var contentY = scrollView.contentOffset.useContents { y }
    private var lastTranslationY = 0.0
    private var tracking = false
    private var nativeGesture = false
    private var correcting = false
    private var handlingScrollCallback = false
    private var blockingNativeDeceleration = false
    private var endVelocity = 0f
    private var disposed = false

    init {
        scrollView.alwaysBounceVertical = true
        scrollView.alwaysBounceHorizontal = false
        scrollView.directionalLockEnabled = true
        scrollView.delegate = this
        // UIScrollView must retain ownership of its built-in pan recognizer's delegate.
        scrollView.panGestureRecognizer.addTarget(this, panSelector)
    }

    fun touchDown(event: UIEvent) {
        if (disposed) return
        val touches = event.allTouches
        if (!touches.isNullOrEmpty() && touches.none { (it as? UITouch)?.phase == UITouchPhase.UITouchPhaseBegan }) return
        coordinator.cancel()
        if (tracking || scrollView.panGestureRecognizer.numberOfTouches > 0uL || (event.allTouches?.size ?: 0) > 1) {
            nativeGesture = true
            tracking = false
        } else {
            nativeGesture = false
            axis = DragAxis.Pending
            lastTranslationY = 0.0
        }
        blockingNativeDeceleration = false
        endVelocity = 0f
        contentY = boundedY(scrollView.contentOffset.useContents { y })
    }

    @ObjCAction
    fun panChanged(pan: UIPanGestureRecognizer) {
        if (disposed) return
        if (pan.numberOfTouches > 1uL) handOverToNative()
        when (pan.state) {
            UIGestureRecognizerStateBegan, UIGestureRecognizerStateChanged -> consumePan()
            UIGestureRecognizerStateCancelled, UIGestureRecognizerStateFailed -> cancelGesture()
        }
    }

    override fun scrollViewWillBeginDragging(scrollView: UIScrollView) {
        delegateFor("scrollViewWillBeginDragging:")?.scrollViewWillBeginDragging(scrollView)
        coordinator.cancel()
        tracking = true
        contentY = boundedY(scrollView.contentOffset.useContents { y })
        consumePan()
    }

    override fun scrollViewDidScroll(scrollView: UIScrollView) {
        if (disposed || correcting) return
        handlingScrollCallback = true
        try {
            if (blockingNativeDeceleration && scrollView.decelerating) {
                setContentY(contentY, stopDeceleration = true)
            } else if (tracking && !nativeGesture) {
                consumePan()
            } else {
                // Loads, reading-position restoration and layout changes never drive Compose parents.
                contentY = boundedY(scrollView.contentOffset.useContents { y })
            }
        } finally {
            handlingScrollCallback = false
        }
        delegateFor("scrollViewDidScroll:")?.scrollViewDidScroll(scrollView)
    }

    private fun consumePan() {
        val pan = scrollView.panGestureRecognizer
        if (pan.numberOfTouches > 1uL) handOverToNative()
        if (!tracking || nativeGesture) return
        if (axis == DragAxis.Pending) {
            pan.velocityInView(null).useContents {
                if (x == 0.0 && y == 0.0) return
                axis = if (abs(y) >= abs(x)) DragAxis.Vertical else DragAxis.Horizontal
            }
        }
        if (axis != DragAxis.Vertical) return
        val translation = pan.translationInView(null).useContents { y }
        val delta = ((translation - lastTranslationY) * density).toFloat()
        lastTranslationY = translation
        coordinator.scroll(delta, NestedScrollSource.UserInput, ::scrollContent)
        // Native scrolling may run before or after the pan target; always keep its result equal
        // to the allocated document position. Recursive didScroll callbacks are suppressed.
        setContentY(contentY)
    }

    private fun scrollContent(delta: Float): Float {
        val before = boundedY(contentY)
        val requested = before - delta.toDouble() / density.toDouble()
        val target = boundedY(requested)
        contentY = target
        setContentY(target)
        val actual = boundedY(scrollView.contentOffset.useContents { y })
        // Retain subpixel distance when UIKit rounds the requested point offset to pixels.
        contentY = if (abs(actual - target) * density <= 0.5) target else actual
        if (contentY == requested) return delta
        return ((before - contentY) * density).toFloat().coerceIn(minOf(0f, delta), maxOf(0f, delta))
    }

    private fun boundedY(y: Double): Double {
        val top = scrollView.adjustedContentInset.useContents { -top }
        val bottom = maxOf(top, scrollView.contentSize.useContents { height } -
            scrollView.bounds.useContents { size.height } + scrollView.adjustedContentInset.useContents { bottom })
        return y.coerceIn(top, bottom)
    }

    private fun setContentY(y: Double, stopDeceleration: Boolean = false) {
        if (!stopDeceleration && scrollView.contentOffset.useContents { this.y } == y) return
        correcting = true
        try {
            scrollView.setContentOffset(CGPointMake(scrollView.contentOffset.useContents { x }, y), animated = false)
        } finally {
            correcting = false
        }
        if (!handlingScrollCallback) delegateFor("scrollViewDidScroll:")?.scrollViewDidScroll(scrollView)
    }

    private fun handOverToNative() {
        coordinator.cancel()
        nativeGesture = true
        tracking = false
        lastTranslationY = 0.0
        endVelocity = 0f
    }

    override fun scrollViewWillEndDragging(
        scrollView: UIScrollView,
        withVelocity: CValue<CGPoint>,
        targetContentOffset: CPointer<CGPoint>?,
    ) {
        delegateFor("scrollViewWillEndDragging:withVelocity:targetContentOffset:")
            ?.scrollViewWillEndDragging(scrollView, withVelocity, targetContentOffset)
        if (!tracking || nativeGesture || axis != DragAxis.Vertical) return
        consumePan()
        endVelocity = scrollView.panGestureRecognizer.velocityInView(null).useContents { (y * density).toFloat() }
        targetContentOffset?.pointed?.y = contentY
        blockingNativeDeceleration = true
    }

    override fun scrollViewDidEndDragging(scrollView: UIScrollView, willDecelerate: Boolean) {
        delegateFor("scrollViewDidEndDragging:willDecelerate:")?.scrollViewDidEndDragging(scrollView, willDecelerate)
        val controlled = tracking && !nativeGesture && axis == DragAxis.Vertical
        tracking = false
        if (controlled) {
            setContentY(contentY, stopDeceleration = true)
            coordinator.startFling(endVelocity, ::scrollContent)
        }
    }

    override fun scrollViewDidEndDecelerating(scrollView: UIScrollView) {
        blockingNativeDeceleration = false
        delegateFor("scrollViewDidEndDecelerating:")?.scrollViewDidEndDecelerating(scrollView)
    }

    override fun scrollViewDidEndScrollingAnimation(scrollView: UIScrollView) {
        delegateFor("scrollViewDidEndScrollingAnimation:")?.scrollViewDidEndScrollingAnimation(scrollView)
    }

    override fun viewForZoomingInScrollView(scrollView: UIScrollView): UIView? =
        delegateFor("viewForZoomingInScrollView:")?.viewForZoomingInScrollView(scrollView)

    override fun scrollViewDidZoom(scrollView: UIScrollView) {
        delegateFor("scrollViewDidZoom:")?.scrollViewDidZoom(scrollView)
    }

    override fun scrollViewWillBeginZooming(scrollView: UIScrollView, withView: UIView?) {
        handOverToNative()
        delegateFor("scrollViewWillBeginZooming:withView:")?.scrollViewWillBeginZooming(scrollView, withView)
    }

    override fun scrollViewDidEndZooming(scrollView: UIScrollView, withView: UIView?, atScale: Double) {
        delegateFor("scrollViewDidEndZooming:withView:atScale:")?.scrollViewDidEndZooming(scrollView, withView, atScale)
    }

    override fun scrollViewWillBeginDecelerating(scrollView: UIScrollView) {
        delegateFor("scrollViewWillBeginDecelerating:")?.scrollViewWillBeginDecelerating(scrollView)
    }

    override fun scrollViewShouldScrollToTop(scrollView: UIScrollView): Boolean {
        cancelGesture()
        return delegateFor("scrollViewShouldScrollToTop:")?.scrollViewShouldScrollToTop(scrollView) ?: true
    }

    override fun scrollViewDidScrollToTop(scrollView: UIScrollView) {
        delegateFor("scrollViewDidScrollToTop:")?.scrollViewDidScrollToTop(scrollView)
    }

    override fun scrollViewDidChangeAdjustedContentInset(scrollView: UIScrollView) {
        contentY = boundedY(scrollView.contentOffset.useContents { y })
        delegateFor("scrollViewDidChangeAdjustedContentInset:")?.scrollViewDidChangeAdjustedContentInset(scrollView)
    }

    private fun delegateFor(selector: String): UIScrollViewDelegateProtocol? =
        previousDelegate?.takeIf { it.respondsToSelector(NSSelectorFromString(selector)) }

    fun cancelGesture() {
        coordinator.cancel()
        tracking = false
        nativeGesture = false
        blockingNativeDeceleration = false
        axis = DragAxis.Pending
        lastTranslationY = 0.0
        endVelocity = 0f
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        cancelGesture()
        scrollView.panGestureRecognizer.removeTarget(this, panSelector)
        if (scrollView.delegate === this) {
            scrollView.delegate = previousDelegate
            scrollView.alwaysBounceVertical = previousBounceVertical
            scrollView.alwaysBounceHorizontal = previousBounceHorizontal
            scrollView.directionalLockEnabled = previousDirectionalLock
        }
    }
}

private class IosWebViewPort(private val configuration: WebViewConfiguration) : WebViewPort {
    override var onMessage: ((String) -> Unit)? = null
    override var onLoad: ((WebLoadEvent) -> Unit)? = null
    override var onNavigation: ((WebNavigationRequest) -> WebNavigationDecision)? = null
    private var destroyed = false
    private var detached = true
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
        preferences.elementFullscreenEnabled = true
        allowsInlineMediaPlayback = true
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
        view.closeAllMediaPresentationsWithCompletionHandler(null)
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

    fun attach() {
        detached = false
    }

    override fun detach() {
        detached = true
        view.closeAllMediaPresentationsWithCompletionHandler {
            // WebKit may return its fullscreen view to the old container during dismissal.
            if (detached) view.removeFromSuperview()
        }
        view.removeFromSuperview()
    }

    override suspend fun closeMediaPresentations() {
        suspendCancellableCoroutine<Unit> { continuation ->
            view.closeAllMediaPresentationsWithCompletionHandler {
                if (detached) view.removeFromSuperview()
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

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
