package com.graveyard.core.model.news

enum class ArticleReplyOrder(val apiValue: String) {
    Ascending("asc"),
    Descending("desc"),
}

/** The main comment returned alongside a page of replies. */
data class ArticleRepliesMetadata(
    val mainComment: ArticleComment,
    val isManager: Boolean,
)
