package com.graveyard.core.designsystem.webview

/** Commands target the mounted content view; calls after unmount do nothing. */
class RichContentController {
    internal var scrollToTopCommand: (() -> Unit)? = null

    fun scrollToTop() {
        scrollToTopCommand?.invoke()
    }
}
