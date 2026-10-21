package com.graveyard.feature.news.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ArticleRoute(
    val articleId: String,
    val articleTitle: String,
    val replyNum: String = "",
    val likeNum: String = "",
    /** Distinguishes repeated in-article pushes for Navigation 3 entry identity. */
    val instanceId: String = "",
) : NavKey
