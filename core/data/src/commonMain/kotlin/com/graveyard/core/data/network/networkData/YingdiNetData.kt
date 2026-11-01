package com.graveyard.core.data.network.networkData

import com.fleeksoft.ksoup.Ksoup
import com.graveyard.core.data.local.preferences.UserSettings
import com.graveyard.core.data.network.YingdiData
import com.graveyard.core.data.network.YingdiApiExecutor
import com.graveyard.core.data.network.model.ArticleContentResponse
import com.graveyard.core.data.network.model.CommentGroup
import com.graveyard.core.data.network.model.CommentListResponse
import com.graveyard.core.data.network.model.NewsListResponse
import com.graveyard.core.data.network.model.TagDetail
import com.graveyard.core.data.network.model.TopContentResponse
import com.graveyard.core.data.network.model.UserInfoResponse
import com.graveyard.core.data.network.sign.YingdiSignedForm
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * 营地数据源：PC Web 接口使用签名 form 表单并返回 JSON，移动站文章通过 GET 返回 HTML。
 *
 * 签名表单的请求头、时间戳、签名和不带 charset 的 Content-Type 由 `YingdiSignPlugin` 补齐，
 * HTML 请求单独设置 Accept，这里负责业务参数和响应解析。
 */
internal class YingdiNetData(
    private val client: HttpClient,
    private val json: Json,
    private val requests: YingdiApiExecutor,
    private val settings: UserSettings,
) : YingdiData {

    private val mobileBaseUrl: String = "https://mob.iyingdi.com"

    /** @param tagId 标签 id，例如首页运营位用 `99`。 */
    override suspend fun getBannerList(tagId: Int): TopContentResponse {
        return requests.post(
            "/web/feed/top-content",
            YingdiSignedForm.of("tag_id" to tagId.toString()),
            expectedArrayField = "top_content",
        )
    }

    override suspend fun getNewsList(
        page: Int,
        size: Int,
        tagId: Int,
        version: Long,
    ): NewsListResponse {
        return requests.post(
            "/web/feed/tag-content-list",
            YingdiSignedForm.of(
                "tag_id" to tagId.toString(),
                "size" to size.toString(),
                "page" to page.toString(),
                "version" to version.toString(),
            ),
            expectedArrayField = "list",
        )
    }

    override suspend fun getTagList(): List<TagDetail> {
        return requests.post("/web/tag/main-tag-bar", YingdiSignedForm.of())
    }

    /**
     * 移动站文章页是 Nuxt SSR：服务端从 Cookie 读取身份，再用 `yd_token` 作为 `Login-Token`
     * 拉详情，所以登录态下要带上和浏览器一致的三项身份 Cookie，才能拿到当前用户的互动状态。
     *
     * token 过期时 SSR 会静默降级为游客页（不返回 200002），这里不刷新也不重试：
     * 只有 SSR 识别出的 `state.user.user_id` 与本地会话一致时才返回 [ArticleContentResponse.viewer]。
     */
    override suspend fun getArticleContentHtml(postId: Long): ArticleContentResponse {
        val url = "$mobileBaseUrl/post/$postId"
        val session = settings.session.first()
        val response = client.get(url) {
            accept(ContentType.Text.Html)
            if (session != null) {
                // 不带 If-None-Match，避免 304 空 body；埋点 Cookie 与浏览器 Sec-* 头都不需要。
                header(
                    HttpHeaders.Cookie,
                    "yd_token=${session.loginToken}; " +
                        "yd_refresh_token=${session.refreshToken}; " +
                        "userid=${session.userId}",
                )
            }
        }
        if (!response.status.isSuccess()) {
            throw DataException(DataError.Http(statusCode = response.status.value))
        }

        val body = response.bodyAsText()
        currentCoroutineContext().ensureActive()
        return withContext(Dispatchers.Default) {
            currentCoroutineContext().ensureActive()
            val document = Ksoup.parse(html = body, baseUri = url)
            val contentHtml = document.selectFirst(".rich-content")?.html()
                ?: throw DataException(DataError.Parsing)
            val page = parseArticlePage(document, json)
            currentCoroutineContext().ensureActive()
            ArticleContentResponse(
                html = contentHtml,
                newsAuthor = page.author,
                replyNum = page.replyNum,
                likeNum = page.likeNum,
                hateNum = page.hateNum,
                viewer = page.viewer?.takeIf { session != null && page.ssrUserId == session.userId },
            )
        }
    }

    /**
     * 用户主页信息：`POST /web/user/user-info`。
     *
     * 响应是平铺对象而不是数组列表，所以这里**不能**传 `expectedArrayField`：
     * 传了会把合法的平铺响应判成解析失败。字段级校验交给 DTO 默认值，
     * 业务错误由共用响应解析的 retCode / retMsg 分支识别。
     */
    override suspend fun getUserInfo(userId: Long): UserInfoResponse {
        return requests.post("/web/user/user-info", YingdiSignedForm.of("user_id" to userId.toString()))
    }

    /**
     * 评论列表（游标分页）：`POST /web/post/comment-list`。
     *
     * - [lastCommentId] 首页传 `0`；后续游标必须来自上次响应原始 `normal_comments` 末项的
     *   `main_comment.comment.id`，在映射、去重和展示处理之前读取，不能使用热评或回复 id。
     * - 切换 [order] 或 [voteFaction] 时，调用方必须把游标重置为 `0` 并清空已有数据。
     * - 首页返回完整 `hot_comments`；后续页的 `hot_comments` 是占位字段，调用方应忽略。
     * - `hot_num` 和 `normal_num` 是对应评论列表的总量；普通评论累计数量达到 `normal_num` 或返回空列表时结束分页。
     *
     * @param order 排序方式，只接受 `hot`（热门优先）或 `created`（按时间）。
     * @param voteFaction 表态/阵营过滤，`0` 表示不按阵营过滤。
     */
    override suspend fun getArticleCommentList(
        postId: Long,
        lastCommentId: Long,
        order: String,
        size: Int,
        voteFaction: Int,
    ): CommentListResponse {
        return requests.post(
            "/web/post/comment-list",
            YingdiSignedForm.of(
                "post_id" to postId.toString(),
                "last_comment_id" to lastCommentId.toString(),
                "order" to order,
                "size" to size.toString(),
                "vote_faction" to voteFaction.toString(),
            ),
            expectedArrayField = "normal_comments",
            allowNullArray = true,
        )
    }

    /**
     * 获取更多评论
     *
     * @param order `desc` 为时间倒序，`asc` 为时间正序。
     * */
    override suspend fun getCommentMore(
        commentId: Long,
        lastReplyId: Long,
        order: String,
        size: Int
    ): CommentGroup {
        return requests.post(
            "/web/post/comment-more",
            YingdiSignedForm.of(
                "comment_id" to commentId.toString(),
                "last_reply_id" to lastReplyId.toString(),
                "order" to order,
                "size" to size.toString(),
            ),
        )
    }
}
