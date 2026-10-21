package com.graveyard.core.data.network.model

import com.graveyard.core.model.news.NewsAuthor

internal data class ArticleContentResponse(
    val html: String,
    val newsAuthor: NewsAuthor,
)
