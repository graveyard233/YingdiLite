package com.graveyard.core.data.network.networkData

import com.graveyard.core.data.network.YingdiData
import com.graveyard.core.data.network.sign.YingdiSignedForm
import com.graveyard.core.model.news.TopContentResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

/**
 * 营地 PC Web 接口的实现，请求体是 form 表单 + `sign` 签名。
 *
 * 请求头、时间戳、签名和不带 charset 的 Content-Type 都由 `YingdiSignPlugin` 补齐，
 * 这里只写业务参数和响应解析。
 */
internal class YingdiNetData(private val client: HttpClient) : YingdiData {

    val commonBasUrl: String = "https://api.iyingdi.com"

    /** @param tagId 标签 id，例如首页运营位用 `99`。 */
    override suspend fun getBannerList(tagId: Int): TopContentResponse {
        val response = client.post("$commonBasUrl/web/feed/top-content") {
            setBody(YingdiSignedForm.of("tag_id" to tagId.toString()))
        }

        // 失败时返回 {"retCode":...,"retMsg":...}，字段与 TopContentResponse 毫无交集，
        // 配合 ignoreUnknownKeys 会被静默反序列化成「空 banner 列表」，所以必须显式挡掉。
        check(response.status.isSuccess()) {
            "营地 top-content 接口返回 HTTP ${response.status}：${response.bodyAsText()}"
        }

        return response.body()
    }
}
