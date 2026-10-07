package com.graveyard.core.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 新闻列表接口响应体。
 *
 * 对应 feed 列表接口返回的 `{ "version": ..., "list": [...] }` 结构。
 */
@Serializable
internal data class NewsListResponse(
    val version: Long = 0L,
    val list: List<NewsListItem> = emptyList(),
)

/**
 * 新闻列表中的一项，包含发布者和文章内容。
 */
@Serializable
internal data class NewsListItem(
    val author: NewsAuthor = NewsAuthor(),
    val feed: NewsFeed = NewsFeed(),
)

/**
 * 新闻作者信息。
 */
@Serializable
internal data class NewsAuthor(
    val id: Long = 0L,
    val username: String = "",
    val head: String = "",
    val credits: Long = 0L,
    @SerialName("badge_json")
    val badgeJson: String = "",
)

/**
 * 新闻文章信息。
 *
 * `tag_json`、`deck_info_json`、`vote_option_json` 和 `badge_json` 等字段
 * 在接口中是 JSON 字符串，因此这里保留为 String，交由业务层按需二次解析。
 */
@Serializable
internal data class NewsFeed(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val type: Long = 0L,
    val style: String = "",
    val imgs: String = "",
    val cover: String = "",
    val url: String = "",
    @SerialName("tag_json")
    val tagJson: String = "",
    @SerialName("like_num")
    val likeNum: String = "",
    @SerialName("reply_num")
    val replyNum: String = "",
    @SerialName("deck_info_json")
    val deckInfoJson: String = "",
    @SerialName("vote_option_json")
    val voteOptionJson: String = "",
    @SerialName("video_url")
    val videoUrl: String = "",
    @SerialName("source_id")
    val sourceId: String = "",
    @SerialName("remuneration_created")
    val remunerationCreated: Long = 0L,
    @SerialName("show_time")
    val showTime: Long = 0L,
)

/**
 * 新闻标签条目。
 *
 * 既是接口 `tag_json` 的二次解析 DTO，也作为本地用户设置（订阅标签列表）的存储模型，
 * JSON 形状固定为 `{"tag": <名称>, "id": <ID>}`；新增字段必须带默认值以兼容旧数据。
 */
@Serializable
data class NewsArticleTag(
    @SerialName("tag")
    val label: String = "",
    val id: Int = 0
)
