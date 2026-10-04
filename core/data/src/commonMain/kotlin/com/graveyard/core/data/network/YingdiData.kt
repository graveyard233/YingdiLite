package com.graveyard.core.data.network

import com.graveyard.core.data.network.model.NewsListResponse
import com.graveyard.core.data.network.model.TopContentResponse

internal interface YingdiData {
    suspend fun getBannerList(tagId: Int): TopContentResponse

    /** Use version 0 for page 1, then reuse the version returned by that response. */
    suspend fun getNewsList(
        page: Int,
        size: Int,
        tagId: Int,
        version: Long = 0L,
    ): NewsListResponse
}
