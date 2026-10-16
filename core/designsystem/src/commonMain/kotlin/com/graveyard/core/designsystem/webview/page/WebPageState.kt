package com.graveyard.core.designsystem.webview.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

sealed interface WebPageSource {
    data class Html(val html: String, val baseUrl: String) : WebPageSource
    data class Url(val url: String) : WebPageSource
}

data class WebPageError(val message: String, val httpStatus: Int? = null, val processTerminated: Boolean = false)

class WebPageState internal constructor() {
    var currentUrl: String? by mutableStateOf(null)
        internal set
    var isLoading: Boolean by mutableStateOf(false)
        internal set
    var error: WebPageError? by mutableStateOf(null)
        internal set
    var canGoBack: Boolean by mutableStateOf(false)
        internal set
    var canGoForward: Boolean by mutableStateOf(false)
        internal set
    internal var owner: Any? = null
    internal var command: ((WebPageCommand) -> Unit)? = null

    fun reload() { command?.invoke(WebPageCommand.Reload) }
    fun goBack() { if (canGoBack) command?.invoke(WebPageCommand.Back) }
    fun goForward() { if (canGoForward) command?.invoke(WebPageCommand.Forward) }
}

internal enum class WebPageCommand { Reload, Back, Forward }

@Composable
fun rememberWebPageState(): WebPageState = remember { WebPageState() }
