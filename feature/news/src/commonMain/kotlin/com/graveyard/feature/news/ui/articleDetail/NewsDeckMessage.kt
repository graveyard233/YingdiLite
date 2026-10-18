package com.graveyard.feature.news.ui.articleDetail

import com.graveyard.core.designsystem.webview.RichContentEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Actions emitted by the deck card in `news_detail.html` through `templateMessage`. */
internal sealed interface NewsDeckMessage {
    data class Copy(val code: String) : NewsDeckMessage
    data class Link(val type: String?, val game: String?, val id: String?) : NewsDeckMessage
}

internal const val NEWS_DECK_MESSAGE = "newsDeck"

/** Returns null for unrelated, malformed, or incomplete deck messages. */
internal fun RichContentEvent.TemplateMessage.asNewsDeckMessage(): NewsDeckMessage? {
    if (name != NEWS_DECK_MESSAGE) return null
    val payload = runCatching { Json.parseToJsonElement(payloadJson) as? JsonObject }.getOrNull() ?: return null
    fun text(key: String) = (payload[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
    return when (text("action")) {
        "copy" -> text("code")?.let(NewsDeckMessage::Copy)
        "link" -> {
            val game = text("game")
            val id = text("id")
            if (game == null && id == null) null else NewsDeckMessage.Link(text("type"), game, id)
        }
        else -> null
    }
}
