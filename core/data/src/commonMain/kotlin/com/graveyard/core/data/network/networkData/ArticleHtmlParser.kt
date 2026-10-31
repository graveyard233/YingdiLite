package com.graveyard.core.data.network.networkData

import com.fleeksoft.ksoup.nodes.Document
import com.graveyard.core.model.news.NewsAuthor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Page metadata parsed from the article detail HTML. */
internal data class ArticlePageMeta(
    val author: NewsAuthor = NewsAuthor(),
    val replyNum: String = "",
    val likeNum: String = "",
)

/** DOM first, then the __NUXT__ payload; page scripts are never evaluated. */
internal fun parseArticlePage(document: Document, json: Json): ArticlePageMeta {
    val author = parseAuthorFromDom(document)
    val counters = parseCountersFromDom(document)
    if (author != null && counters.replyNum.isNotEmpty() && counters.likeNum.isNotEmpty()) {
        return ArticlePageMeta(author, counters.replyNum, counters.likeNum)
    }
    val nuxt = readNuxtPage(document, json)
    return ArticlePageMeta(
        author = author ?: nuxt?.author ?: NewsAuthor(),
        replyNum = counters.replyNum.ifEmpty { nuxt?.replyNum.orEmpty() },
        likeNum = counters.likeNum.ifEmpty { nuxt?.likeNum.orEmpty() },
    )
}

private fun parseAuthorFromDom(document: Document): NewsAuthor? {
    val area = document.selectFirst(".rich-detail .user-title-area") ?: return null
    val username = area.selectFirst(".user-info .name")?.text()?.trim().orEmpty()
    val head = area.selectFirst(".user-head img.avatar")?.absUrl("src")?.trim().orEmpty()
    if (username.isBlank() && head.isBlank()) return null
    val level = area.selectFirst(".user-info .level")?.text()?.trim()
        ?.let { Regex("""(?i)Lv\.\s*(\d{1,3})""").matchEntire(it) }
        ?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..100 } ?: 0
    return NewsAuthor(username = username, head = head, level = level)
}

private data class PageCounters(
    val replyNum: String = "",
    val likeNum: String = "",
)

private fun parseCountersFromDom(document: Document): PageCounters {
    val area = document.selectFirst("#comments-area-id")
        ?: document.selectFirst(".write-area")
        ?: return PageCounters()
    var replyNum = ""
    var likeNum = ""
    for (icon in area.select(".icon")) {
        val iconName = icon.selectFirst("use")
            ?.let { it.attr("xlink:href").ifEmpty { it.attr("href") } }
            .orEmpty()
        val value = icon.selectFirst("em")?.text()?.trim()
            ?.toLongOrNull()?.takeIf { it >= 0 }?.toString()
            ?: continue
        when {
            likeNum.isEmpty() && iconName.contains("icon-fire") -> likeNum = value
            replyNum.isEmpty() && iconName.contains("icon-comment") -> replyNum = value
        }
    }
    if (likeNum.isEmpty() && replyNum.isEmpty()) {
        // `use` 的命名空间属性在 Ksoup 上取不到时的结构兜底。
        replyNum = area.selectFirst("""a[href="#viewComments"] em""")?.text()?.trim()
            ?.toLongOrNull()?.toString().orEmpty()
        likeNum = area.select("span.icon em").firstOrNull()?.text()?.trim()
            ?.toLongOrNull()?.toString().orEmpty()
    }
    return PageCounters(replyNum = replyNum, likeNum = likeNum)
}

private fun readNuxtPage(document: Document, json: Json): ArticlePageMeta? {
    for (script in document.select("script")) {
        val source = script.data()
        if (!source.contains("__NUXT__")) continue
        val page = try {
            NuxtPageReader(source, json).read()
        } catch (_: IllegalArgumentException) {
            null
        }
        if (page != null) return page
    }
    return null
}

/** Reads the fields this page needs from data[0].detail; page scripts are never evaluated. */
private class NuxtPageReader(source: String, private val json: Json) {
    private val tokens = tokenize(source)
    private val bindings = mutableMapOf<String, String>()

    fun read(): ArticlePageMeta? {
        val assignment = tokens.indices.firstOrNull {
            tokens[it] == "window" && tokens.getOrNull(it + 1) == "." &&
                tokens.getOrNull(it + 2) == "__NUXT__" && tokens.getOrNull(it + 3) == "="
        } ?: return null
        val expression = assignment + 4
        val root = if (tokens.getOrNull(expression) == "{") expression else {
            // Nuxt also wraps its data in an IIFE and reuses primitive arguments as aliases.
            val function = if (tokens.getOrNull(expression) == "(") expression + 1 else expression
            if (tokens.getOrNull(function) != "function") return null
            val parameters = function + 1
            if (tokens.getOrNull(parameters) != "(") return null
            val parametersEnd = containerEnd(parameters) ?: return null
            val body = parametersEnd + 1
            if (tokens.getOrNull(body) != "{") return null
            val bodyEnd = containerEnd(body) ?: return null
            var arguments = bodyEnd + 1
            if (tokens.getOrNull(arguments) == ")") arguments++
            readBindings(parameters, parametersEnd, arguments)
            if (tokens.getOrNull(body + 1) != "return") return null
            body + 2
        }

        val data = member(root, "data") ?: return null
        if (tokens.getOrNull(data.first) != "[") return null
        val detail = member(data.first + 1, "detail") ?: return null
        val post = member(detail.first, "post") ?: member(data.first + 1, "post")
        val replyNum = post?.let { readCount(it, "reply_num") }.orEmpty()
        val likeNum = post?.let { readCount(it, "like_num") }.orEmpty()
        val author = readAuthor(detail.first) ?: NewsAuthor()
        if (replyNum.isEmpty() && likeNum.isEmpty() && author == NewsAuthor()) return null
        return ArticlePageMeta(author = author, replyNum = replyNum, likeNum = likeNum)
    }

    private fun readAuthor(detailStart: Int): NewsAuthor? {
        val author = member(detailStart, "author") ?: return null

        fun value(name: String): String? {
            val range = member(author.first, name) ?: return null
            if (range.first != range.last) return null
            val token = tokens[range.first]
            return bindings[token] ?: token
        }
        fun text(name: String): String? = value(name)?.let(::decodeString)

        val id = value("id")?.toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val username = text("username") ?: return null
        val head = text("head") ?: return null
        val credits = value("credits")?.toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val badgeJson = text("badge_json") ?: return null
        if (id == 0L && username.isBlank() && head.isBlank()) return null
        return NewsAuthor(id = id, username = username, head = head, credits = credits, badgeJson = badgeJson)
    }

    private fun readCount(container: IntRange, name: String): String? {
        val range = member(container.first, name) ?: return null
        if (range.first != range.last) return null
        val token = tokens[range.first]
        return (bindings[token] ?: token).toLongOrNull()?.takeIf { it >= 0 }?.toString()
    }

    private fun readBindings(parameters: Int, parametersEnd: Int, arguments: Int) {
        if (tokens.getOrNull(arguments) != "(") return
        var parameter = parameters + 1
        var argument = arguments + 1
        while (parameter < parametersEnd) {
            val end = valueEnd(argument) ?: return
            if (end == argument + 1) bindings[tokens[parameter]] = tokens[argument]
            parameter += 2
            if (tokens.getOrNull(end) != ",") return
            argument = end + 1
        }
    }

    private fun member(start: Int, name: String): IntRange? {
        if (tokens.getOrNull(start) != "{") return null
        var key = start + 1
        while (key < tokens.size && tokens[key] != "}") {
            if (tokens.getOrNull(key + 1) != ":") return null
            val value = key + 2
            val end = valueEnd(value) ?: return null
            if (tokens[key] == name || decodeString(tokens[key]) == name) return value until end
            if (tokens.getOrNull(end) != ",") return null
            key = end + 1
        }
        return null
    }

    private fun valueEnd(start: Int): Int? {
        var index = start
        while (index < tokens.size) {
            when (tokens[index]) {
                ",", "}", "]", ")" -> return index.takeIf { it > start }
                "{", "[", "(" -> index = (containerEnd(index) ?: return null) + 1
                else -> index++
            }
        }
        return null
    }

    private fun containerEnd(start: Int): Int? {
        val stack = mutableListOf<String>()
        for (index in start until tokens.size) {
            when (tokens[index]) {
                "{" -> stack.add("}")
                "[" -> stack.add("]")
                "(" -> stack.add(")")
                "}", "]", ")" -> {
                    if (stack.lastOrNull() != tokens[index]) return null
                    stack.removeAt(stack.lastIndex)
                    if (stack.isEmpty()) return index
                }
            }
        }
        return null
    }

    private fun decodeString(token: String): String? {
        if (!token.startsWith('"')) return null
        return try {
            json.decodeFromString<String>(token)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

/** Keeps strings and comments opaque so braces or author-like text inside them cannot match. */
private fun tokenize(source: String): List<String> {
    val tokens = mutableListOf<String>()
    var index = 0
    while (index < source.length) {
        val start = index
        val char = source[index]
        when {
            char.isWhitespace() -> index++
            source.startsWith("//", index) -> {
                index += 2
                while (index < source.length && source[index] != '\n' && source[index] != '\r') index++
            }
            source.startsWith("/*", index) -> {
                val end = source.indexOf("*/", index + 2)
                if (end < 0) return emptyList()
                index = end + 2
            }
            char == '"' || char == '\'' || char == '`' -> {
                index++
                var closed = false
                while (index < source.length) {
                    if (source[index] == '\\') {
                        index += 2
                    } else if (source[index++] == char) {
                        closed = true
                        break
                    }
                }
                if (!closed) return emptyList()
                tokens.add(source.substring(start, index))
            }
            char.isLetterOrDigit() || char == '_' || char == '$' || char == '-' -> {
                index++
                while (index < source.length && (
                    source[index].isLetterOrDigit() || source[index] == '_' || source[index] == '$'
                )) index++
                tokens.add(source.substring(start, index))
            }
            else -> {
                tokens.add(char.toString())
                index++
            }
        }
    }
    return tokens
}
