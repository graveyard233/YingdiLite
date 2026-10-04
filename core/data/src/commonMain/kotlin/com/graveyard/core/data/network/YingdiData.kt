package com.graveyard.core.data.network

import com.graveyard.core.data.network.model.NewsListResponse
import com.graveyard.core.data.network.model.TopContentResponse

internal interface YingdiData {
    suspend fun getBannerList(tagId: Int): TopContentResponse

    suspend fun getNewsList(page: Int, size: Int, tagId: Int): NewsListResponse
}
