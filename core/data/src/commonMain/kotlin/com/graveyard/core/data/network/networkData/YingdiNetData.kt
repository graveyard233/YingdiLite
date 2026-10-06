package com.graveyard.core.data.network.networkData

import com.graveyard.core.data.network.YingdiData
import com.graveyard.core.data.network.model.NewsListResponse
import com.graveyard.core.data.network.model.TopContentResponse
import com.graveyard.core.data.network.sign.YingdiSignedForm
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * 营地 PC Web 接口的实现，请求体是 form 表单 + `sign` 签名。
 *
 * 请求头、时间戳、签名和不带 charset 的 Content-Type 都由 `YingdiSignPlugin` 补齐，
 * 这里只写业务参数和响应解析。
 */
internal class YingdiNetData(
    private val client: HttpClient,
    private val json: Json,
) : YingdiData {

    private val commonBaseUrl: String = "https://api.iyingdi.com"

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

    private suspend inline fun <reified T> HttpResponse.decodeResponse(
        expectedArrayField: String,
    ): T {
        if (!status.isSuccess()) {
            throw DataException(DataError.Http(statusCode = status.value))
        }

        val body = bodyAsText()
        currentCoroutineContext().ensureActive()
        return withContext(Dispatchers.Default) {
            currentCoroutineContext().ensureActive()
            val payload = json.parseToJsonElement(body) as? JsonObject
                ?: throw DataException(DataError.Parsing)

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
            if (payload[expectedArrayField] !is JsonArray) {
                throw DataException(DataError.Parsing)
            }

            currentCoroutineContext().ensureActive()
            val result = json.decodeFromJsonElement<T>(payload)
            currentCoroutineContext().ensureActive()
            result
        }
    }
}
