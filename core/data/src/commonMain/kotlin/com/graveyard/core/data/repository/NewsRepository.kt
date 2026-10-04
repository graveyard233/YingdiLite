package com.graveyard.core.data.repository

import androidx.paging.PagingData
import com.graveyard.core.data.result.DataResult
import com.graveyard.core.model.news.NewsArticle
import com.graveyard.core.model.news.NewsBanner
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    suspend fun getBanners(tagId: Int): DataResult<List<NewsBanner>>

    /** The caller selects the tag and owns caching this flow in its lifecycle scope. */
    fun getNews(
        tagId: Int,
        pageSize: Int = 20,
    ): Flow<PagingData<NewsArticle>>
}
