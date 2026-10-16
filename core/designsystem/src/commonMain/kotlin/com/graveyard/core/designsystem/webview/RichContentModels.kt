package com.graveyard.core.designsystem.webview

data class RichContentTheme(val background: String, val foreground: String, val link: String, val dark: Boolean)

sealed interface RichContentEvent {
    data object Rendered : RichContentEvent
    data object Error : RichContentEvent
    data class LinkClick(val url: String) : RichContentEvent
    data class ImageClick(val urls: List<String>, val index: Int) : RichContentEvent
    data class TemplateMessage(val name: String, val payloadJson: String) : RichContentEvent
}

internal data class ReadingKey(val templateId: String, val revision: Int, val protocol: Int, val contentKey: String)

/** Route-owned CSS pixel offsets; pooled views never own a route's reading position. */
class RichContentReadingState {
    private data class Position(var offset: Double = 0.0, var owner: Long = 0L)
    private val positions = mutableMapOf<ReadingKey, Position>()
    private var serial = 0L

    internal inner class Binding(private val key: ReadingKey, private val token: Long) {
        val offset: Double get() = positions[key]?.offset ?: 0.0
        fun update(value: Double) {
            val position = positions[key] ?: return
            if (position.owner == token && value.isFinite()) position.offset = value.coerceAtLeast(0.0)
        }
    }

    internal fun bind(key: ReadingKey): Binding {
        val token = ++serial
        positions.getOrPut(key) { Position() }.owner = token
        return Binding(key, token)
    }
}
