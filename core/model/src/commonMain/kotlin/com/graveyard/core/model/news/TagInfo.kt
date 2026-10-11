package com.graveyard.core.model.news

import kotlinx.serialization.Serializable

@Serializable
data class TagInfo(
    val id: Int,
    val title: String,
    val alias: String,
    val iconUrl: String,
    val fontColor: String,
    val fontColorDark: String,
)
