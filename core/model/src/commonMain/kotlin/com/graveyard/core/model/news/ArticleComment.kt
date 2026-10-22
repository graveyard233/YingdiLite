package com.graveyard.core.model.news

/** A comment from a news article, mapped from the network response. */
data class ArticleComment(
    val id: Long,
    val content: String,
    val imageUrls: List<String>,
    val replyNum: Int,
    val likeNum: Int,
    val hateNum: Int,
    /** Unix timestamp in seconds. */
    val created: Long,
    val receiverId: Long,
    val receiverName: String,
    val authorDelete: Boolean,
    val isVisible: Boolean,
    val ipHome: String,
    val action: Int,
    val author: NewsAuthor,
)

/** A main comment and the reply preview returned with it. */
data class ArticleCommentThread(
    val mainComment: ArticleComment,
    val replies: List<ArticleComment>,
    val isManager: Boolean,
)

enum class ArticleCommentOrder(val apiValue: String) {
    Hot("hot"),
    Created("created"),
}

/** Metadata returned alongside each comment-list response. */
data class ArticleCommentsMetadata(
    val hotComments: List<ArticleCommentThread> = emptyList(),
    val hotNum: Int = 0,
    val normalNum: Int = 0,
)
