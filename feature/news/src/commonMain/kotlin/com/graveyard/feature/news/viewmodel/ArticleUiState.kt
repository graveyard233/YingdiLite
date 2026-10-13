package com.graveyard.feature.news.viewmodel

data class ArticleUiState(
    val articleId: String,
    val articleTitle: String,
    val detailPlaceholder: String = "新闻详情内容占位",
    val commentsPlaceholder: String = "评论区内容占位",
)
