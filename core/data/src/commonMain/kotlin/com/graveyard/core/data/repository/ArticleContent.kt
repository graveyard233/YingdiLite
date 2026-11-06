package com.graveyard.core.data.repository

import com.graveyard.core.model.news.NewsAuthor
import kotlinx.serialization.Serializable

data class ArticleContent(
    val articleId: Long,
    val html: String,
    val baseUrl: String,
    val newsAuthor: NewsAuthor = NewsAuthor(),
    val replyNum: String = "",
    val likeNum: String = "",
    val hateNum: String = "",
    /** 当前登录用户与文章的关系；游客或 SSR 未识别登录态时为 null。 */
    val viewer: ArticleViewerState? = null,
)

/** 当前用户对文章的态度，来自 `detail.user.action`。 */
@Serializable
enum class ArticleVote { None, Like, Hate }

/** 当前登录用户与文章的关系，来自详情页 `__NUXT__` 的 `data[0].detail.user`。 */
data class ArticleViewerState(
    val vote: ArticleVote = ArticleVote.None,
    /** `action` 原始值：0 = 无、1 = 赞、2 = 踩（已抓包确认），其他值映射为 [ArticleVote.None]。 */
    val rawAction: Int = 0,
    val collected: Boolean = false,
    val followAuthor: Boolean = false,
    val isManager: Boolean = false,
    val voteFaction: Int = 0,
)
