package com.graveyard.core.data.local

import com.graveyard.core.model.news.NewsArticle
import com.graveyard.core.model.news.NewsBanner

internal interface NewsLocalDataSource {
    // Null means uncached; an empty page is a cached response.
    suspend fun readPage(tagId: Int, pageSize: Int, page: Int): CachedNewsPage?

    suspend fun writePage(
        tagId: Int,
        pageSize: Int,
        page: Int,
        data: CachedNewsPage,
    )

    // Null means uncached; an empty list is a cached response.
    suspend fun readBanners(tagId: Int): List<NewsBanner>?

    suspend fun writeBanners(tagId: Int, banners: List<NewsBanner>)

    /** Removes all page sizes, pages and banners for this tag. */
    suspend fun clearTag(tagId: Int)
}

internal data class CachedNewsPage(
    val articles: List<NewsArticle>,
    val endReached: Boolean,
)
