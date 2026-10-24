package com.graveyard.feature.news.ui.articleDetail

import com.graveyard.core.designsystem.webview.RichContentEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Actions emitted by user cards in `news_detail.html` through `templateMessage`. */
internal data class NewsUserCardOpen(val id: String, val username: String)

internal const val NEWS_USER_CARD_MESSAGE = "newsUserCard"

private val USER_ID = Regex("[0-9]{1,20}")

/** Returns null for unrelated, malformed, or incomplete user card messages. */
internal fun RichContentEvent.TemplateMessage.asNewsUserCardOpen(): NewsUserCardOpen? {
    if (name != NEWS_USER_CARD_MESSAGE) return null
    val payload = runCatching { Json.parseToJsonElement(payloadJson) as? JsonObject }.getOrNull() ?: return null
    fun text(key: String) = (payload[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
    if (text("action") != "open") return null
    val id = text("id")?.takeIf { USER_ID.matches(it) && it.any { digit -> digit != '0' } } ?: return null
    return NewsUserCardOpen(id = id, username = text("username")?.take(64).orEmpty())
}
