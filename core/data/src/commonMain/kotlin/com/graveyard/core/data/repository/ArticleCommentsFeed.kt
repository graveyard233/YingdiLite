package com.graveyard.core.data.repository

import androidx.paging.PagingData
import com.graveyard.core.model.news.ArticleCommentThread
import com.graveyard.core.model.news.ArticleCommentsMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** A paging session for one article and one comment ordering. */
data class ArticleCommentsFeed(
    val comments: Flow<PagingData<ArticleCommentThread>>,
    val state: StateFlow<ArticleCommentsFeedState>,
)

/**
 * Metadata of the latest first page that was fetched successfully.
 *
 * The repository only replaces [metadata] when a page of the current session [generation]
 * succeeds, so a failed refresh keeps the previous value and an invalidated source can never
 * overwrite a newer session. Rendering this out of band keeps the paging data a plain list of
 * comments, without a synthetic header entry.
 */
data class ArticleCommentsFeedState(
    val metadata: ArticleCommentsMetadata? = null,
    val generation: Long = 0,
)
