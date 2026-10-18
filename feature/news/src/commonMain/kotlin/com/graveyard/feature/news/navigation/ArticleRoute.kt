package com.graveyard.feature.news.navigation

import androidx.navigation3.runtime.NavKey
import com.graveyard.core.model.news.NewsAuthor
import kotlinx.serialization.Serializable

@Serializable
data class ArticleRoute(
    val articleId: String,
    val articleTitle: String,
    val newsAuthor: NewsAuthor = NewsAuthor(),
    val replyNum: String = "",
    val likeNum: String = "",
) : NavKey
