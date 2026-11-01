package com.graveyard.core.data.network.networkData

import com.fleeksoft.ksoup.nodes.Document
import com.graveyard.core.data.repository.ArticleViewerState
import com.graveyard.core.data.repository.ArticleVote
import com.graveyard.core.model.news.NewsAuthor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Page metadata parsed from the article detail HTML. */
internal data class ArticlePageMeta(
    val author: NewsAuthor = NewsAuthor(),
    val replyNum: String = "",
    val likeNum: String = "",
    val hateNum: String = "",
    /** 当前用户与文章的关系；页面没有 `detail.user` 时为 null。 */
    val viewer: ArticleViewerState? = null,
    /** SSR 认定的登录用户（`state.user.user_id`）；游客页为 null，用于校验 [viewer] 是否可信。 */
    val ssrUserId: Long? = null,
)

/**
 * 作者和计数 DOM 优先、__NUXT__ 兜底；踩数和用户互动状态只存在于 __NUXT__。
 * 页面脚本不会被执行。
 */
internal fun parseArticlePage(document: Document, json: Json): ArticlePageMeta {
    val author = parseAuthorFromDom(document)
    val counters = parseCountersFromDom(document)
    // 互动状态只在 __NUXT__ 里，因此即使 DOM 齐全也要读取。
    val nuxt = readNuxtPage(document, json)
    return ArticlePageMeta(
        author = author ?: nuxt?.author ?: NewsAuthor(),
        replyNum = counters.replyNum.ifEmpty { nuxt?.replyNum.orEmpty() },
        likeNum = counters.likeNum.ifEmpty { nuxt?.likeNum.orEmpty() },
        hateNum = nuxt?.hateNum.orEmpty(),
        viewer = nuxt?.viewer,
        ssrUserId = nuxt?.ssrUserId,
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
        val hateNum = post?.let { readCount(it, "hate_num") }.orEmpty()
        val author = readAuthor(detail.first) ?: NewsAuthor()
        val viewer = readViewer(detailStart = detail.first, dataItemStart = data.first + 1)
        val ssrUserId = readSsrUserId(root)
        if (replyNum.isEmpty() && likeNum.isEmpty() && hateNum.isEmpty() &&
            author == NewsAuthor() && viewer == null
        ) return null
        return ArticlePageMeta(
            author = author,
            replyNum = replyNum,
            likeNum = likeNum,
            hateNum = hateNum,
            viewer = viewer,
            ssrUserId = ssrUserId,
        )
    }

    private fun readAuthor(detailStart: Int): NewsAuthor? {
        val author = member(detailStart, "author") ?: return null

        fun value(name: String): String? = scalar(author, name)
        fun text(name: String): String? = value(name)?.let(::decodeString)

        val id = value("id")?.toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val username = text("username") ?: return null
        val head = text("head") ?: return null
        val credits = value("credits")?.toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val badgeJson = text("badge_json") ?: return null
        if (id == 0L && username.isBlank() && head.isBlank()) return null
        return NewsAuthor(id = id, username = username, head = head, credits = credits, badgeJson = badgeJson)
    }

    /**
     * `data[0].detail.user` 是当前用户与帖子的关系。`action`：0 = 无、1 = 赞、2 = 踩
     * （2026-10-31 用同一账号对同一帖子赞 / 踩抓包确认）。
     * `data[0].isPostLike` 名字像布尔，实际与 `action` 取值完全一致（0/1/2），只在缺少 `action` 时兜底。
     * 两者都不存在时返回 null。
     */
    private fun readViewer(detailStart: Int, dataItemStart: Int): ArticleViewerState? {
        val user = member(detailStart, "user")?.takeIf { tokens.getOrNull(it.first) == "{" }
        val isPostLike = member(dataItemStart, "isPostLike")?.let(::resolve)?.toIntOrNull()
        if (user == null && isPostLike == null) return null
        val action = user?.let { readInt(it, "action") } ?: isPostLike ?: 0
        val vote = when (action) {
            1 -> ArticleVote.Like
            2 -> ArticleVote.Hate
            else -> ArticleVote.None
        }
        return ArticleViewerState(
            vote = vote,
            rawAction = action,
            collected = user?.let { readBool(it, "collected") } ?: false,
            followAuthor = user?.let { readBool(it, "follow_author") } ?: false,
            isManager = user?.let { readBool(it, "is_manager") } ?: false,
            voteFaction = user?.let { readInt(it, "vote_faction") } ?: 0,
        )
    }

    /** `state.user.user_id`；游客页为 `null`（或别名到 null），此时返回 null。 */
    private fun readSsrUserId(root: Int): Long? {
        val state = member(root, "state") ?: return null
        val user = member(state.first, "user") ?: return null
        return scalar(user, "user_id")?.toLongOrNull()?.takeIf { it > 0 }
    }

    private fun readCount(container: IntRange, name: String): String? =
        scalar(container, name)?.toLongOrNull()?.takeIf { it >= 0 }?.toString()

    private fun readInt(container: IntRange, name: String): Int? =
        scalar(container, name)?.toIntOrNull()

    private fun readBool(container: IntRange, name: String): Boolean? =
        member(container.first, name)?.let(::truthy)

    /** 单 token 值，并还原 IIFE 参数别名；多 token 表达式返回 null。 */
    private fun scalar(container: IntRange, name: String): String? {
        val range = member(container.first, name) ?: return null
        return resolve(range)
    }

    private fun resolve(range: IntRange): String? {
        if (range.first != range.last) return null
        val token = tokens[range.first]
        return bindings[token] ?: token
    }

    /**
     * 布尔值兼容 `true` / `false`、压缩写法 `!0` / `!1`，以及数字（非 0 为真）。
     * 无法识别的值返回 null。
     */
    private fun truthy(range: IntRange): Boolean? {
        if (range.last == range.first + 1 && tokens[range.first] == "!") {
            val operand = bindings[tokens[range.last]] ?: tokens[range.last]
            return truthyLiteral(operand)?.not()
        }
        return resolve(range)?.let(::truthyLiteral)
    }

    private fun truthyLiteral(value: String): Boolean? = when (value) {
        "true" -> true
        "false", "null" -> false
        else -> value.toLongOrNull()?.let { it != 0L }
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
