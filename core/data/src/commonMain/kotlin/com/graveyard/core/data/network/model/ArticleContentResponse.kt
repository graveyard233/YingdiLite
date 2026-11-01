package com.graveyard.core.data.network.model

import com.graveyard.core.data.repository.ArticleViewerState
import com.graveyard.core.model.news.NewsAuthor

internal data class ArticleContentResponse(
    val html: String,
    val newsAuthor: NewsAuthor,
    val replyNum: String = "",
    val likeNum: String = "",
    val hateNum: String = "",
    /** 仅当请求带登录 Cookie 且 SSR 识别出同一用户时非空。 */
    val viewer: ArticleViewerState? = null,
)
