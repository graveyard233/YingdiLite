package com.graveyard.core.data.repository

import androidx.paging.PagingData
import com.graveyard.core.model.news.ArticleComment
import com.graveyard.core.model.news.ArticleRepliesMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** A paging session for one main comment and one reply ordering. */
data class ArticleRepliesFeed(
    val replies: Flow<PagingData<ArticleComment>>,
    val state: StateFlow<ArticleRepliesFeedState>,
)

/** Keeps the latest successful main comment across refreshes, guarded by source generation. */
data class ArticleRepliesFeedState(
    val metadata: ArticleRepliesMetadata? = null,
    val generation: Long = 0,
)
