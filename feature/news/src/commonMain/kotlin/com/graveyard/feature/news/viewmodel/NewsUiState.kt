package com.graveyard.feature.news.viewmodel

import com.graveyard.core.data.result.DataError
import com.graveyard.core.model.news.NewsBanner

data class NewsUiState(
    val selectedTagId: Int? = null,
    val bannersByTag: Map<Int, BannerUiState> = emptyMap(),
)

data class BannerUiState(
    val items: List<NewsBanner> = emptyList(),
    val isLoading: Boolean = false,
    val error: DataError? = null,
)
