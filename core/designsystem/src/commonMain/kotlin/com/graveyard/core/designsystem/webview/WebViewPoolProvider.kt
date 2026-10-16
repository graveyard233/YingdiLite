package com.graveyard.core.designsystem.webview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import com.graveyard.core.designsystem.webview.platform.rememberWebViewFactory
import com.graveyard.core.designsystem.webview.pool.WebViewPool

internal val LocalWebViewPool = staticCompositionLocalOf<WebViewPool> {
    error("WebViewPoolProvider must be installed at the application root")
}

@Composable
fun WebViewPoolProvider(content: @Composable () -> Unit) {
    val factory = rememberWebViewFactory()
    val scope = rememberCoroutineScope()
    val pool = remember(factory, scope) { WebViewPool(scope, factory) }
    DisposableEffect(pool) { onDispose { pool.close() } }
    CompositionLocalProvider(LocalWebViewPool provides pool, content = content)
}
