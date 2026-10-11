package com.graveyard.core.data.mapper

import com.graveyard.core.data.network.model.TagDetail
import com.graveyard.core.model.news.TagInfo

internal fun TagDetail.toTagInfo(): TagInfo = TagInfo(
    id = id,
    title = title,
    alias = alias,
    iconUrl = icon,
    fontColor = fontColor,
    fontColorDark = nightFontColor,
)
