package com.graveyard.feature.news.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ArticleRepliesRoute(
    val article: ArticleRoute,
    val commentId: Long,
    val instanceId: String,
) : NavKey
