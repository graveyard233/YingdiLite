package com.graveyard.core.data.mapper

import com.fleeksoft.ksoup.Ksoup
import com.graveyard.core.data.network.model.CommentGroup
import com.graveyard.core.data.network.model.CommentItem
import com.graveyard.core.data.network.model.CommentReply
import com.graveyard.core.data.network.model.CommentUser
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException
import com.graveyard.core.model.news.ArticleComment
import com.graveyard.core.model.news.ArticleCommentThread
import com.graveyard.core.model.news.NewsAuthor

private const val COMMENT_LINE_BREAK = "\uE000"
private val breakTag = Regex("(?i)<br\\s*/?>")
private val blockEndTag = Regex("(?i)</(p|div|li|blockquote)\\s*>")
private val scriptOrStyle = Regex("(?is)<(script|style)\\b[^>]*>.*?</\\1\\s*>")
private val horizontalWhitespace = Regex("[ \\t]+")
private val newlineWhitespace = Regex(" *\\n *")

internal fun CommentGroup.toArticleCommentThread(): ArticleCommentThread {
    if (mainComment.comment.id <= 0L) throw DataException(DataError.Parsing)
    return ArticleCommentThread(
        mainComment = mainComment.toArticleComment(),
        replies = replyComments.orEmpty().map { it.toArticleComment() },
        isManager = isManager,
    )
}

private fun CommentReply.toArticleComment(): ArticleComment = comment.toArticleComment(user, action)

private fun CommentItem.toArticleComment(author: CommentUser, action: Int): ArticleComment =
    ArticleComment(
        id = id,
        content = sanitizeCommentContent(content),
        imageUrls = splitCommentImageUrls(imgs),
        replyNum = replyNum,
        likeNum = likeNum,
        hateNum = hateNum,
        created = created,
        receiverId = receiverId,
        receiverName = receiverName,
        authorDelete = authorDelete,
        isVisible = isVisible != 0,
        ipHome = ipHome,
        action = action,
        author = NewsAuthor(
            id = author.id,
            username = author.username,
            head = author.head,
            credits = author.credits,
            badgeJson = author.badgeJson,
        ),
    )

private val cleartextImageUrl = Regex("(?i)^http://")

/**
 * The `imgs` field packs multiple image URLs separated by a backtick, e.g.
 * `https://a/1.jpeg`https://b/2.gif`. Commas are also accepted as a separator
 * for older responses, but only when not part of a URL.
 */
private fun splitCommentImageUrls(raw: String): List<String> =
    raw.split(Regex("[`\\s]+|,(?=\\s*https?://)"))
        .map(String::trim)
        .filter(String::isNotEmpty)
        // Some responses still return cleartext URLs, which Android blocks by default.
        .map { cleartextImageUrl.replaceFirst(it, "https://") }

private fun sanitizeCommentContent(raw: String): String {
    if (raw.isBlank()) return ""
    val withBreakMarkers = raw
        .replace("\r\n", COMMENT_LINE_BREAK)
        .replace("\n", COMMENT_LINE_BREAK)
        .replace("\r", COMMENT_LINE_BREAK)
        .replace(breakTag, COMMENT_LINE_BREAK)
        .replace(blockEndTag, COMMENT_LINE_BREAK)
    val withoutExecutableContent = scriptOrStyle.replace(withBreakMarkers, "")
    val decoded = Ksoup.parse(withoutExecutableContent).text().replace(COMMENT_LINE_BREAK, "\n")
    return newlineWhitespace.replace(horizontalWhitespace.replace(decoded, " "), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}
