package com.graveyard.feature.news.viewmodel

import com.graveyard.core.data.repository.ArticleViewerState
import com.graveyard.core.data.result.DataError
import com.graveyard.feature.news.navigation.ArticleAuthAction

data class ArticleInteractionState(
    val userId: Long? = null,
    val viewer: ArticleViewerState? = null,
    /** Keep the last confirmed presentation after a failure, but re-read before another mutation. */
    val isSynchronized: Boolean = false,
    val likeNum: String = "",
    val hateNum: String = "",
    val isBusy: Boolean = false,
)

enum class ArticleInteractionMessage { SyncFailed, OperationFailed, SavedSyncFailed }

data class ArticleInteractionFeedback(
    val id: Long,
    val message: ArticleInteractionMessage? = null,
    val error: DataError? = null,
    val requiresLogin: ArticleAuthAction? = null,
)
