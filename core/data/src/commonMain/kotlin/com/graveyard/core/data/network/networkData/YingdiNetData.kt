package com.graveyard.core.data.network.networkData

import com.fleeksoft.ksoup.Ksoup
import com.graveyard.core.data.network.YingdiData
import com.graveyard.core.data.network.model.ArticleContentResponse
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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * 营地数据源：PC Web 接口使用签名 form 表单并返回 JSON，移动站文章通过 GET 返回 HTML。
 *
 * 签名表单的请求头、时间戳、签名和不带 charset 的 Content-Type 由 `YingdiSignPlugin` 补齐，
 * HTML 请求单独设置 Accept，这里负责业务参数和响应解析。
 */
internal class YingdiNetData(
    private val client: HttpClient,
    private val json: Json,
) : YingdiData {

    private val commonBaseUrl: String = "https://api.iyingdi.com"
    private val mobileBaseUrl: String = "https://mob.iyingdi.com"

    /** @param tagId 标签 id，例如首页运营位用 `99`。 */
    override suspend fun getBannerList(tagId: Int): TopContentResponse {
        val url = "$commonBaseUrl/web/feed/top-content"
        val response = client.post(url) {
            setBody(YingdiSignedForm.of("tag_id" to tagId.toString()))
        }
        return response.decodeResponse(expectedArrayField = "top_content")
    }

    override suspend fun getNewsList(
        page: Int,
        size: Int,
        tagId: Int,
        version: Long,
    ): NewsListResponse {
        val url = "$commonBaseUrl/web/feed/tag-content-list"
        val response = client.post(url) {
            setBody(
                YingdiSignedForm.of(
                    "tag_id" to tagId.toString(),
                    "size" to size.toString(),
                    "page" to page.toString(),
                    "version" to version.toString(),
                ),
            )
        }
        return response.decodeResponse(expectedArrayField = "list")
    }

    override suspend fun getTagList(): List<TagDetail> {
        val url = "$commonBaseUrl/web/tag/main-tag-bar"
        val response = client.post(url) {
            setBody(YingdiSignedForm.of())
        }

        return response.decodeResponse()
    }

    override suspend fun getArticleContentHtml(postId: Long): ArticleContentResponse {
        val url = "$mobileBaseUrl/post/$postId"
        val response = client.get(url) {
            accept(ContentType.Text.Html)
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
            val author = parseArticleAuthor(document, json)
            currentCoroutineContext().ensureActive()
            ArticleContentResponse(html = contentHtml, newsAuthor = author)
        }
    }

    /**
     * 用户主页信息：`POST /web/user/user-info`。
     *
     * 响应是平铺对象而不是数组列表，所以这里**不能**传 `expectedArrayField`：
     * 传了会把合法的平铺响应判成解析失败。字段级校验交给 DTO 默认值，
     * 业务错误仍由 `decodeResponse` 的 `retCode` / `retMsg` 分支识别。
     */
    override suspend fun getUserInfo(userId: Long): UserInfoResponse {
        val url = "$commonBaseUrl/web/user/user-info"
        val response = client.post(url) {
            setBody(YingdiSignedForm.of("user_id" to userId.toString()))
        }
        return response.decodeResponse()
    }

    /**
     * 评论列表（游标分页）：`POST /web/post/comment-list`。
     *
     * - [lastCommentId] 是**不透明游标**：首页传 `0`，之后必须把服务端上一次返回的游标
     *   原样传回。热门排序下评论 id 会来回跳，客户端不能自己递增、排序或去重后当游标。
     * - 切换 [order] 或 [voteFaction] 时，调用方必须把游标重置为 `0` 并清空已有数据。
     * - 响应同时包含 `hot_comments` 与 `normal_comments` 两个列表，
     *   判断是否还有下一页看 `hot_num + normal_num`，不能用 `size` 比较。
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
        val url = "$commonBaseUrl/web/post/comment-list"
        val response = client.post(url) {
            setBody(
                YingdiSignedForm.of(
                    "post_id" to postId.toString(),
                    "last_comment_id" to lastCommentId.toString(),
                    "order" to order,
                    "size" to size.toString(),
                    "vote_faction" to voteFaction.toString(),
                ),
            )
        }
        // 没有评论时接口可能把两个列表都返回 null，允许 null 而不是报解析失败。
        return response.decodeResponse(
            expectedArrayField = "normal_comments",
            allowNullArray = true,
        )
    }


    /**
     * 校验响应信封并反序列化。
     *
     * @param expectedArrayField 期望存在的数组字段；`null` 表示按顶层结构（对象或数组）解析。
     * @param allowNullArray 为 `true` 时，[expectedArrayField] 存在但值为 `null` 视为空，
     *   交给 DTO 默认值兜底；为 `false` 时 `null` 也算解析失败。
     */
    @PublishedApi
    internal suspend inline fun <reified T> HttpResponse.decodeResponse(
        expectedArrayField: String? = null,
        allowNullArray: Boolean = false,
    ): T {
        if (!status.isSuccess()) {
            throw DataException(DataError.Http(statusCode = status.value))
        }

        val body = bodyAsText()
        currentCoroutineContext().ensureActive()
        return withContext(Dispatchers.Default) {
            currentCoroutineContext().ensureActive()
            val payload = json.parseToJsonElement(body)
            when (payload) {
                is JsonObject -> {
                    if (expectedArrayField != null) {
                        // DTO defaults must not turn error envelopes into successful empty lists.
                        if (expectedArrayField !in payload) {
                            if ("retCode" in payload || "retMsg" in payload) {
                                throw DataException(
                                    DataError.Business(
                                        retCode = (payload["retCode"] as? JsonPrimitive)?.contentOrNull,
                                        retMsg = (payload["retMsg"] as? JsonPrimitive)?.contentOrNull,
                                    ),
                                )
                            }
                            throw DataException(DataError.Parsing)
                        }
                        val field = payload[expectedArrayField]
                        if (field !is JsonArray && !(allowNullArray && field is JsonNull)) {
                            throw DataException(DataError.Parsing)
                        }
                    } else if ("retCode" in payload || "retMsg" in payload) {
                        throw DataException(
                            DataError.Business(
                                retCode = (payload["retCode"] as? JsonPrimitive)?.contentOrNull,
                                retMsg = (payload["retMsg"] as? JsonPrimitive)?.contentOrNull,
                            ),
                        )
                    }
                }
                is JsonArray -> {
                    if (expectedArrayField != null) {
                        throw DataException(DataError.Parsing)
                    }
                }
                else -> throw DataException(DataError.Parsing)
            }

            currentCoroutineContext().ensureActive()
            val result = json.decodeFromJsonElement<T>(payload)
            currentCoroutineContext().ensureActive()
            result
        }
    }
}
