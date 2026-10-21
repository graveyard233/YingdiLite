package com.graveyard.feature.news.viewmodel

import com.graveyard.core.data.repository.ArticleContent
import com.graveyard.core.data.result.DataError
import com.graveyard.core.model.news.NewsAuthor

data class ArticleUiState(
    val articleId: String,
    val articleTitle: String,
    val content: ArticleContent? = null,
    val newsAuthor: NewsAuthor = NewsAuthor(),
    val isLoading: Boolean = true,
    val error: DataError? = null,
    val commentsPlaceholder: String = "评论区内容占位",
)
