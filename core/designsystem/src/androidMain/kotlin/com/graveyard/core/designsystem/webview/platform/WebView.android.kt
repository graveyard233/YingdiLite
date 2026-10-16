package com.graveyard.core.designsystem.webview.platform

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
internal actual fun rememberWebViewFactory(): (WebViewConfiguration) -> WebViewPort {
    val context = LocalContext.current
    return remember(context) { { configuration: WebViewConfiguration -> AndroidWebViewPort(context, configuration) } }
}

@Composable
internal actual fun NativeWebView(port: WebViewPort, bindingId: Long, interaction: WebViewInteraction, modifier: Modifier) {
    val view = (port as AndroidWebViewPort).view
    key(bindingId, interaction) {
        val dispatcher = remember { NestedScrollDispatcher() }
        val connection = remember { object : NestedScrollConnection {} }
        val scope = rememberCoroutineScope()
        AndroidView(
            factory = { context ->
                if (interaction == WebViewInteraction.NestedScroll) WebViewScrollContainer(context, view, dispatcher, scope)
                else FrameLayout(context).apply {
                    (view.parent as? ViewGroup)?.removeView(view)
                    addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }
            },
            modifier = if (interaction == WebViewInteraction.NestedScroll) modifier.nestedScroll(connection, dispatcher) else modifier,
            onRelease = { container -> container.removeAllViews() },
        )
    }
}

private class WebViewScrollContainer(
    context: Context,
    private val webView: WebView,
    private val dispatcher: NestedScrollDispatcher,
    private val scope: CoroutineScope,
) : FrameLayout(context) {
    private enum class DragAxis { Pending, Horizontal, Vertical }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var dragAxis = DragAxis.Pending
    private var downX = 0f
    private var downY = 0f
    private var lastY = 0f
    private var downOriginY = 0f
    private var pointerId = 0
    private var touching = false
    private var reportingScroll = false
    private var dispatchingTouch = false
    private var settling: Job? = null
    private val finishScroll = Runnable {
        reportingScroll = false
        settling = scope.launch {
            dispatcher.dispatchPostFling(Velocity.Zero, Velocity.Zero)
        }
    }

    init {
        (webView.parent as? ViewGroup)?.removeView(webView)
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        webView.setOnScrollChangeListener { _, _, y, _, oldY ->
            if (reportingScroll && !dispatchingTouch && y != oldY) {
                dispatcher.dispatchPostScroll(
                    consumed = Offset(0f, (oldY - y).toFloat()),
                    available = Offset.Zero,
                    source = if (touching) NestedScrollSource.UserInput else NestedScrollSource.SideEffect,
                )
                if (!touching) scheduleScrollEnd()
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                removeCallbacks(finishScroll)
                settling?.cancel()
                touching = true
                reportingScroll = false
                dragAxis = DragAxis.Pending
                downX = event.rawX
                downY = event.rawY
                lastY = event.rawY
                downOriginY = event.rawY - event.y
                pointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragAxis == DragAxis.Pending) {
                    val dx = abs(event.rawX - downX)
                    val dy = abs(event.rawY - downY)
                    if (maxOf(dx, dy) > touchSlop) {
                        dragAxis = if (dy >= dx) DragAxis.Vertical else DragAxis.Horizontal
                    }
                }
                if (dragAxis == DragAxis.Vertical) reportingScroll = true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                dragAxis = DragAxis.Vertical
                reportingScroll = true
            }
        }

        // Claim only vertical drags so the pager can still detect horizontal touch slop.
        super.requestDisallowInterceptTouchEvent(dragAxis == DragAxis.Vertical)
        val fingerDelta = if (
            event.actionMasked == MotionEvent.ACTION_MOVE && dragAxis == DragAxis.Vertical &&
            event.pointerCount == 1 && event.getPointerId(0) == pointerId
        ) event.rawY - lastY else 0f
        lastY = event.rawY
        pointerId = event.getPointerId(0)

        // Collapsing the app bar moves the viewport; keep WebView's touch coordinates continuous.
        val adjusted = MotionEvent.obtain(event)
        if (dragAxis == DragAxis.Vertical) {
            adjusted.offsetLocation(0f, event.rawY - event.y - downOriginY)
        }
        val beforeScrollY = webView.scrollY
        val handled = try {
            dispatchingTouch = true
            super.dispatchTouchEvent(adjusted)
        } finally {
            dispatchingTouch = false
            adjusted.recycle()
        }
        if (fingerDelta != 0f) {
            // Split this gesture sample into WebView-consumed and boundary remainder so the app bar
            // can start expanding in the same drag that brings the document to its top.
            val consumed = (beforeScrollY - webView.scrollY).toFloat()
            val available = if (fingerDelta > 0f && !webView.canScrollVertically(-1)) {
                (fingerDelta - consumed).coerceAtLeast(0f)
            } else {
                0f
            }
            dispatcher.dispatchPostScroll(
                consumed = Offset(0f, consumed),
                available = Offset(0f, available),
                source = NestedScrollSource.UserInput,
            )
        }
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            touching = false
            super.requestDisallowInterceptTouchEvent(false)
            if (reportingScroll) scheduleScrollEnd()
        }
        return handled
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // WebView's own requests must not override the direction chosen for this gesture.
        super.requestDisallowInterceptTouchEvent(touching && dragAxis == DragAxis.Vertical)
    }

    private fun scheduleScrollEnd() {
        removeCallbacks(finishScroll)
        // Native WebView owns the fling; snap the app bar once its scroll callbacks go quiet.
        postDelayed(finishScroll, 120L)
    }

    override fun onViewRemoved(child: View) {
        if (child === webView) {
            webView.setOnScrollChangeListener(null)
            removeCallbacks(finishScroll)
            settling?.cancel()
            reportingScroll = false
        }
        super.onViewRemoved(child)
    }
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
    val view = WebView(context).apply {
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
