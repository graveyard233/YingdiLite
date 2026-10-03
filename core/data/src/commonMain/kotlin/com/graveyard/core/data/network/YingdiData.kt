package com.graveyard.core.data.network

import com.graveyard.core.model.news.TopContentResponse

interface YingdiData {
    suspend fun getBannerList(tagId: Int): TopContentResponse
}