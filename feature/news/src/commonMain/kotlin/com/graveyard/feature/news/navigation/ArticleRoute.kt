package com.graveyard.feature.news.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ArticleRoute(val articleId: Long) : NavKey
