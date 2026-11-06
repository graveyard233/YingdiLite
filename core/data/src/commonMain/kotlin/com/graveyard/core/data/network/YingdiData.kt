package com.graveyard.core.data.network

import com.graveyard.core.data.network.model.ArticleContentResponse
import com.graveyard.core.data.network.model.CommentGroup
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

    /** Returns the article's rich-content HTML, author, and interaction counts parsed from the same page. */
    suspend fun getArticleContentHtml(postId: Long): ArticleContentResponse

    /** 用户主页信息。响应是平铺对象，不是列表。 */
    suspend fun getUserInfo(userId: Long): UserInfoResponse

    /**
     * 评论列表（游标分页）。首页 [lastCommentId] 传 `0`；后续游标必须取上次响应
     * `normal_comments` 列表末项的 `main_comment.comment.id`，不能从热评、回复或重排后的列表推导。
     * 切换 `order` 或 `voteFaction` 时重置为 `0`。首屏包含完整热评；后续响应的热评字段是占位数据，
     * 调用方应忽略。`hot_num` 和 `normal_num` 是各自列表的总量。
     *
     * @param order `hot` 为综合排序，`created` 为按创建时间排序。
     */
    suspend fun getArticleCommentList(
        postId: Long,
        lastCommentId: Long,
        order: String,
        size: Int,
        voteFaction: Int,
    ): CommentListResponse

    /**
     * First page uses cursor 0; subsequent cursors come from the last raw reply's comment ID.
     * An empty or short reply page ends pagination. Changing order resets the cursor to 0.
     * @param order `desc` 为时间倒序，`asc` 为时间正序。
     * */
    suspend fun getCommentMore(
        commentId: Long,
        lastReplyId: Long,
        order: String,
        size: Int = 10,
    ): CommentGroup

    /**
     * 收藏 / 取消收藏文章：`POST /web/post/collect`。
     *
     * 接口没有额外参数，服务端按当前收藏状态取反，所以同一个方法同时承担收藏和取消收藏；
     * 成功时无响应体，失败抛 [com.graveyard.core.data.result.DataException]。
     */
    suspend fun toggleArticleCollect(postId: Long, expectedUserId: Long? = null)

    /**
     * 点赞 / 点踩 / 取消：`POST /web/post/like`。
     *
     * 三种行为共用同一接口，由 [action] 决定；成功时无响应体。
     *
     * @param action `0` 无（取消）、`1` 赞、`2` 踩。
     */
    suspend fun setArticleVote(postId: Long, action: Int, expectedUserId: Long? = null)

    /**
     * 评论点赞 / 点踩及取消：`POST /web/post/like-comment`，成功时无响应体。
     *
     * @param action `1` 点赞、`-1` 取消点赞、`2` 点踩、`-2` 取消点踩，按原值传递。
     */
    suspend fun setCommentLike(
        postId: Long,
        commentId: Long,
        action: Int,
        expectedUserId: Long? = null,
    )
}
