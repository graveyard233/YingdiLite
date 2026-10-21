package com.graveyard.core.data.network

import com.graveyard.core.data.network.model.ArticleContentResponse
import com.graveyard.core.data.network.model.CommentListResponse
import com.graveyard.core.data.network.model.NewsListResponse
import com.graveyard.core.data.network.model.TagDetail
import com.graveyard.core.data.network.model.TopContentResponse
import com.graveyard.core.data.network.model.UserInfoResponse

internal interface YingdiData {
    suspend fun getBannerList(tagId: Int): TopContentResponse

    /** Use version 0 for page 1, then reuse the version returned by that response. */
    suspend fun getNewsList(
        page: Int,
        size: Int,
        tagId: Int,
        version: Long = 0L,
    ): NewsListResponse

    suspend fun getTagList(): List<TagDetail>

    /** Returns the article's rich-content HTML and author parsed from the same page. */
    suspend fun getArticleContentHtml(postId: Long): ArticleContentResponse

    /** 用户主页信息。响应是平铺对象，不是列表。 */
    suspend fun getUserInfo(userId: Long): UserInfoResponse

    /**
     * 评论列表（游标分页）。[lastCommentId] 首页传 `0`，之后必须原样回传服务端给的上一个游标；
     * 切换 `order` 或 `voteFaction` 时重置为 `0`。
     *
     * @param order 只接受 `hot` 或 `time`。
     */
    suspend fun getArticleCommentList(
        postId: Long,
        lastCommentId: Long,
        order: String,
        size: Int,
        voteFaction: Int,
    ): CommentListResponse


}
