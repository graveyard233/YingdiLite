package com.graveyard.core.model.news

import com.graveyard.core.model.user.calculateUserLevel
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class NewsAuthor(
    val id: Long = 0L,
    val username: String = "",
    val head: String = "",
    val credits: Long = 0L,
    val badgeJson: String = "",
    // HTML can supply a level without credits; 0 means the level is unavailable.
    @Transient
    val level: Int = calculateUserLevel(credits),
)
