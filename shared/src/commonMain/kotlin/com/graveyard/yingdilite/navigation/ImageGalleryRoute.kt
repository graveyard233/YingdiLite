package com.graveyard.yingdilite.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/** Each opening has its own stable identity, including after saved-state restoration. */
@Serializable
data class ImageGalleryRoute(
    val urls: List<String>,
    val initialIndex: Int,
    val instanceId: String = Uuid.random().toString(),
) : NavKey
