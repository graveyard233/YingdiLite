package com.graveyard.feature.news.ui.articleDetail

import com.graveyard.core.designsystem.webview.RichContentEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Actions emitted by the article link card in `news_detail.html` through `templateMessage`. */
internal data class NewsCardOpen(val id: String, val type: String, val title: String, val author: String)

internal const val NEWS_CARD_MESSAGE = "newsCard"

/**
 * `data-typee` value of an in-app article. Checked against the live pages: for
 * `https://mob.iyingdi.com/post/<data-id>` the post title matches the card title, so `data-id` is
 * the post id. Other types have no in-app screen and are routed to the external link in JS.
 */
internal const val NEWS_CARD_TYPE_ARTICLE = "13"

private val POST_ID = Regex("[0-9]{1,20}")

/** Returns null for unrelated, malformed, or non-article card messages. */
internal fun RichContentEvent.TemplateMessage.asNewsCardOpen(): NewsCardOpen? {
    if (name != NEWS_CARD_MESSAGE) return null
    val payload = runCatching { Json.parseToJsonElement(payloadJson) as? JsonObject }.getOrNull() ?: return null
    fun text(key: String) = (payload[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
    if (text("action") != "open") return null
    val id = text("id")?.takeIf { POST_ID.matches(it) } ?: return null
    val type = text("type") ?: return null
    if (type != NEWS_CARD_TYPE_ARTICLE) return null
    // Card text is display-only; cap it so a hostile document cannot inflate the nav key.
    return NewsCardOpen(
        id = id,
        type = type,
        title = text("title")?.take(200).orEmpty(),
        author = text("author")?.take(64).orEmpty(),
    )
}
