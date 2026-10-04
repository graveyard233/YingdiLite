package com.graveyard.core.model.news

/** Embedded JSON, counters and timestamps retain their original values and types. */
data class NewsArticle(
    val id: Long = 0L,
    val author: NewsAuthor = NewsAuthor(),
    val title: String = "",
    val content: String = "",
    val type: Long = 0L,
    val style: String = "",
    val imgs: String = "",
    val cover: String = "",
    val url: String = "",
    val tagJson: String = "",
    val likeNum: String = "",
    val replyNum: String = "",
    val deckInfoJson: String = "",
    val voteOptionJson: String = "",
    val videoUrl: String = "",
    val sourceId: String = "",
    val remunerationCreated: Long = 0L,
    val showTime: Long = 0L,
)
