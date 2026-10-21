package com.graveyard.core.data.repository

import com.graveyard.core.model.news.NewsAuthor

data class ArticleContent(
    val articleId: Long,
    val html: String,
    val baseUrl: String,
    val newsAuthor: NewsAuthor = NewsAuthor(),
)
