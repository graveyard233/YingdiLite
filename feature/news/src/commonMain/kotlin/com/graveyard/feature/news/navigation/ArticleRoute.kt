package com.graveyard.feature.news.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ArticleRoute(
    val articleId: String,
    val articleTitle: String,
) : NavKey
