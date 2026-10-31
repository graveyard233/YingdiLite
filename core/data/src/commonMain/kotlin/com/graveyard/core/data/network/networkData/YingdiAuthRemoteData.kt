package com.graveyard.core.data.network.networkData

import com.graveyard.core.data.network.YINGDI_API_BASE_URL
import com.graveyard.core.data.network.decodeYingdiResponse
import com.graveyard.core.data.network.model.LoginResponse
import com.graveyard.core.data.network.model.RefreshLoginResponse
import com.graveyard.core.data.network.sign.YingdiSignedForm
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json

/** 认证接口直接使用底层客户端，跳过自动刷新执行器。 */
internal class YingdiAuthRemoteData(private val client: HttpClient, private val json: Json) {
    suspend fun login(username: String, password: String): LoginResponse =
        client.post("$YINGDI_API_BASE_URL/web/user/login") {
            setBody(YingdiSignedForm.of("username" to username, "password" to password, "type" to "password"))
        }.decodeYingdiResponse(json)

    suspend fun refreshLogin(refreshToken: String): RefreshLoginResponse =
        client.post("$YINGDI_API_BASE_URL/web/user/refresh-login") {
            setBody(YingdiSignedForm.of("refresh_token" to refreshToken))
        }.decodeYingdiResponse(json)
}
