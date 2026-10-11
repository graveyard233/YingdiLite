package com.graveyard.core.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagDetail(
    val id: Int,
    val title: String,
    val alias: String,

    @SerialName("font_color")
    val fontColor: String,

    @SerialName("night_font_color")
    val nightFontColor: String,

    @SerialName("bg_img")
    val bgImg: String,

    @SerialName("night_bg_img")
    val nightBgImg: String,

    val img: String = "",

    @SerialName("img_selected")
    val imgSelected: String = "",

    val url: String,
    val icon: String
)
