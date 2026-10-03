package com.graveyard.core.model.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `POST https://api.iyingdi.com/web/feed/top-content` 的响应体，这里只映射 banner 部分。
 *
 * 响应里还有一个 `supplement` 数组（作者 + 帖子），当前用不到所以没有建模；
 * 解析时请使用 `Json { ignoreUnknownKeys = true }`，否则该字段会导致解析失败。
 */
@Serializable
data class TopContentResponse(
    @SerialName("top_content")
    val topContent: List<BannerItem> = emptyList(),
)

/**
 * 顶部运营位（banner）条目，对应响应中的 `top_content` 数组元素。
 */
@Serializable
data class BannerItem(
    /** 运营位 id，如 `2749`。 */
    @SerialName("ad_id")
    val adId: Long = 0L,
    /** banner 图片地址。 */
    val img: String = "",
    /** 点击跳转地址，可能是 http(s) 链接，也可能是 `wanxiu://` 之类的 App 内 scheme。 */
    val url: String = "",
    /** banner 标题。 */
    val title: String = "",
)
