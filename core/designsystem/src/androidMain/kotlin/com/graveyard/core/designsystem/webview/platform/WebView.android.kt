package com.graveyard.core.designsystem.webview.platform

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal actual fun rememberWebViewFactory(): (WebViewConfiguration) -> WebViewPort {
    val context = LocalContext.current
    return remember(context) { { configuration: WebViewConfiguration -> AndroidWebViewPort(context, configuration) } }
}

@Composable
internal actual fun NativeWebView(port: WebViewPort, bindingId: Long, interaction: WebViewInteraction, modifier: Modifier) {
    val view = (port as AndroidWebViewPort).view
    val density = LocalDensity.current.density
    key(bindingId, interaction, density) {
        val dispatcher = remember { NestedScrollDispatcher() }
        val connection = remember { object : NestedScrollConnection {} }
        val scope = rememberCoroutineScope()
        val decay = rememberSplineBasedDecay<Float>()
        val coordinator = remember(dispatcher, scope, decay) { WebViewScrollCoordinator(dispatcher, scope, decay) }
        AndroidView(
            factory = { context ->
                if (interaction == WebViewInteraction.NestedScroll) WebViewScrollContainer(context, view, coordinator)
                else FrameLayout(context).apply {
                    (view.parent as? ViewGroup)?.removeView(view)
                    addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }
            },
            modifier = if (interaction == WebViewInteraction.NestedScroll) modifier.nestedScroll(connection, dispatcher) else modifier,
            onRelease = { container ->
                coordinator.cancel()
                container.removeAllViews()
            },
        )
    }
}

private class WebViewScrollContainer(
    context: Context,
    private val webView: ScrollableWebView,
    private val coordinator: WebViewScrollCoordinator,
) : FrameLayout(context) {
    private enum class DragAxis { Pending, Horizontal, Vertical }

    private val configuration = ViewConfiguration.get(context)
    private val touchSlop = configuration.scaledTouchSlop
    private var dragAxis = DragAxis.Pending
    private var downX = 0f
    private var downY = 0f
    private var lastY = 0f
    private var downOriginY = 0f
    private var pointerId = 0
    private var touching = false
    private var nativeGesture = false
    private var parentConsumedY = 0f
    private var contentY: Double? = null
    private var correcting = false
    private var velocityTracker: VelocityTracker? = null

    init {
        (webView.parent as? ViewGroup)?.removeView(webView)
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        webView.setOnScrollChangeListener { _, _, _, _, _ ->
            if (!correcting && ((touching && !nativeGesture && dragAxis == DragAxis.Vertical) || coordinator.isFlinging)) {
                // Chromium can apply a touch sample asynchronously. Only the touch adapter and
                // shared decay allocate document distance; native callbacks cannot consume it again.
                correctContentOffset()
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                coordinator.cancel()
                touching = true
                nativeGesture = false
                parentConsumedY = 0f
                contentY = null
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                dragAxis = DragAxis.Pending
                downX = event.rawX
                downY = event.rawY
                lastY = event.rawY
                downOriginY = event.rawY - event.y
                pointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!nativeGesture && dragAxis == DragAxis.Pending) {
                    val dx = abs(event.rawX - downX)
                    val dy = abs(event.rawY - downY)
                    if (maxOf(dx, dy) > touchSlop) {
                        dragAxis = if (dy >= dx) DragAxis.Vertical else DragAxis.Horizontal
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
                coordinator.cancel()
                nativeGesture = true
                contentY = null
                lastY = event.rawY
                velocityTracker?.clear()
            }
        }

        // Claim only vertical drags so the pager can still detect horizontal touch slop.
        val vertical = !nativeGesture && dragAxis == DragAxis.Vertical
        super.requestDisallowInterceptTouchEvent(nativeGesture || vertical)
        val raw = MotionEvent.obtain(event)
        raw.offsetLocation(event.rawX - event.x, event.rawY - event.y)
        velocityTracker?.addMovement(raw)
        raw.recycle()
        val fingerDelta = if (
            event.actionMasked == MotionEvent.ACTION_MOVE && vertical &&
            event.pointerCount == 1 && event.getPointerId(0) == pointerId
        ) event.rawY - lastY else 0f
        lastY = event.rawY
        pointerId = event.getPointerId(0)

        if (vertical && contentY == null) {
            contentY = webView.scrollY.toDouble()
            webView.cancelLongPress()
        }
        val preConsumed = if (fingerDelta != 0f) coordinator.preScroll(fingerDelta, NestedScrollSource.UserInput) else 0f
        parentConsumedY += preConsumed

        // Collapsing the app bar moves the viewport; keep WebView's touch coordinates continuous.
        val adjusted = MotionEvent.obtain(event)
        adjusted.offsetLocation(0f, event.rawY - event.y - downOriginY - parentConsumedY)
        if (event.actionMasked == MotionEvent.ACTION_UP && vertical) {
            // Preserve native taps and selection, but never let native and Compose flings race.
            adjusted.action = MotionEvent.ACTION_CANCEL
        }
        val handled = try {
            super.dispatchTouchEvent(adjusted)
        } finally {
            adjusted.recycle()
        }
        if (fingerDelta != 0f) {
            val remaining = fingerDelta - preConsumed
            val consumed = scrollContent(remaining)
            parentConsumedY += coordinator.postScroll(consumed, remaining - consumed, NestedScrollSource.UserInput)
        }
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            touching = false
            super.requestDisallowInterceptTouchEvent(false)
            if (event.actionMasked == MotionEvent.ACTION_UP && vertical) {
                velocityTracker?.computeCurrentVelocity(1000, configuration.scaledMaximumFlingVelocity.toFloat())
                val velocity = velocityTracker?.getYVelocity(pointerId) ?: 0f
                coordinator.startFling(
                    if (abs(velocity) >= configuration.scaledMinimumFlingVelocity) velocity else 0f,
                    ::scrollContent,
                )
            } else if (event.actionMasked == MotionEvent.ACTION_CANCEL) coordinator.cancel()
            velocityTracker?.recycle()
            velocityTracker = null
        }
        return handled
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // WebView's own requests must not override the direction chosen for this gesture.
        super.requestDisallowInterceptTouchEvent(touching && (nativeGesture || dragAxis == DragAxis.Vertical))
    }

    private fun scrollContent(delta: Float): Float {
        val maximum = webView.maximumScrollY.toDouble()
        val before = (contentY ?: webView.scrollY.toDouble()).coerceIn(0.0, maximum)
        val requested = before - delta.toDouble()
        val target = requested.coerceIn(0.0, maximum)
        contentY = target
        correctContentOffset()
        // Keep fractions in contentY so pixel rounding neither loses distance nor invents a
        // boundary on slow decay frames. Account for any additional clamping by WebView itself.
        val actual = if (webView.scrollY == target.roundToInt()) target else webView.scrollY.toDouble()
        contentY = actual
        // Subtracting absolute offsets can manufacture a remainder even without a boundary.
        if (actual == requested) return delta
        return (before - actual).toFloat().coerceIn(minOf(0f, delta), maxOf(0f, delta))
    }

    private fun correctContentOffset() {
        val target = contentY?.roundToInt()?.coerceIn(0, webView.maximumScrollY) ?: return
        if (webView.scrollY == target) return
        correcting = true
        try {
            webView.scrollTo(webView.scrollX, target)
        } finally {
            correcting = false
        }
    }

    private fun dispose() {
        coordinator.cancel()
        velocityTracker?.recycle()
        velocityTracker = null
        touching = false
        contentY = null
    }

    override fun onDetachedFromWindow() {
        dispose()
        super.onDetachedFromWindow()
    }

    override fun onViewRemoved(child: View) {
        if (child === webView) {
            webView.setOnScrollChangeListener(null)
            dispose()
        }
        super.onViewRemoved(child)
    }
}

private class ScrollableWebView(context: Context) : WebView(context) {
    val maximumScrollY: Int
        get() = (computeVerticalScrollRange() - computeVerticalScrollExtent()).coerceAtLeast(0)
}

@SuppressLint("SetJavaScriptEnabled")
private class AndroidWebViewPort(context: Context, configuration: WebViewConfiguration) : WebViewPort {
    override var onMessage: ((String) -> Unit)? = null
    override var onLoad: ((WebLoadEvent) -> Unit)? = null
    override var onNavigation: ((WebNavigationRequest) -> WebNavigationDecision)? = null
    private val main = Handler(Looper.getMainLooper())
    private var destroyed = false
    private var epoch = 0L
    private var navigation = 0L
    private val popups = mutableSetOf<WebView>()
    val view = ScrollableWebView(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        settings.javaScriptEnabled = configuration.javaScript
        settings.domStorageEnabled = configuration.domStorage
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setSupportMultipleWindows(true)
        isHorizontalScrollBarEnabled = !configuration.templateBridge
        if (configuration.templateBridge) addJavascriptInterface(Bridge(), "AppWebBridge")
        webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                if (destroyed) return false
                val popup = WebView(context)
                popups += popup
                var handled = false
                fun handle(url: String) {
                    if (handled || url == "about:blank") return
                    handled = true
                    if (!destroyed && onNavigation?.invoke(WebNavigationRequest(url, true)) == WebNavigationDecision.Allow) loadUrl(url)
                    popup.post { popups.remove(popup); popup.destroy() }
                }
                popup.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (request.isForMainFrame) handle(request.url.toString())
                        return true
                    }
                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) { handle(url) }
                }
                (resultMsg.obj as WebView.WebViewTransport).webView = popup
                resultMsg.sendToTarget()
                return true
            }
        }
    }

    private inner class Client(private val generation: Long) : WebViewClient() {
        private var mainUrl: String? = null
        private fun current() = !destroyed && generation == epoch

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!current()) return true
            if (!request.isForMainFrame) return false
            return onNavigation?.invoke(WebNavigationRequest(request.url.toString())) != WebNavigationDecision.Allow
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            if (!current()) return
            mainUrl = url
            onLoad?.invoke(WebLoadEvent.Started(++navigation, url))
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (current() && mainUrl == url) onLoad?.invoke(
                WebLoadEvent.Finished(navigation, url, view.canGoBack(), view.canGoForward()),
            )
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (current() && request.isForMainFrame && (mainUrl == null || request.url.toString() == mainUrl)) {
                onLoad?.invoke(WebLoadEvent.Failed(navigation, error.description.toString()))
            }
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            if (current() && request.isForMainFrame && (mainUrl == null || request.url.toString() == mainUrl)) {
                onLoad?.invoke(WebLoadEvent.Failed(navigation, errorResponse.reasonPhrase ?: "HTTP error", errorResponse.statusCode))
            }
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            if (!destroyed) onLoad?.invoke(WebLoadEvent.ProcessTerminated)
            return true
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun postMessage(message: String) {
            main.post { if (!destroyed) onMessage?.invoke(message) }
        }
    }

    private fun begin() {
        view.webViewClient = Client(++epoch)
    }

    override fun loadHtml(html: String, baseUrl: String) {
        if (destroyed) return
        begin()
        view.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
    }

    override fun loadUrl(url: String) { if (!destroyed) { begin(); view.loadUrl(url) } }
    override fun reload() { if (!destroyed) { begin(); view.reload() } }
    override fun goBack() { if (!destroyed && view.canGoBack()) { begin(); view.goBack() } }
    override fun goForward() { if (!destroyed && view.canGoForward()) { begin(); view.goForward() } }
    override fun stopLoading() { if (!destroyed) { epoch++; view.stopLoading() } }
    override fun evaluate(script: String) {
        if (!destroyed) runCatching { view.evaluateJavascript(script, null) }.onFailure {
            onLoad?.invoke(WebLoadEvent.Failed(navigation, "JavaScript evaluation failed"))
        }
    }
    override fun detach() { (view.parent as? ViewGroup)?.removeView(view) }

    override fun destroy() {
        if (destroyed) return
        destroyed = true
        view.stopLoading()
        onMessage = null
        onLoad = null
        onNavigation = null
        popups.forEach { it.destroy() }
        popups.clear()
        main.removeCallbacksAndMessages(null)
        view.removeJavascriptInterface("AppWebBridge")
        view.webViewClient = WebViewClient()
        view.webChromeClient = null
        detach()
        view.destroy()
    }
}
