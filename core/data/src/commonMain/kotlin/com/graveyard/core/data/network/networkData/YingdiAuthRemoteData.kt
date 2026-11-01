package com.graveyard.core.data.network.networkData

import com.graveyard.core.data.crypto.YingdiSignProfile
import com.graveyard.core.data.network.YINGDI_API_BASE_URL
import com.graveyard.core.data.network.decodeYingdiResponse
import com.graveyard.core.data.network.model.LoginResponse
import com.graveyard.core.data.network.model.RefreshLoginResponse
import com.graveyard.core.data.network.sign.YingdiSignedForm
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json

/**
 * 登录与刷新 token 接口，使用 mweb 签名（[YingdiSignProfile.Mweb]）：
 * 改用 PC 的 /web/user/-- 会顶掉 PC 网页上的登录态。
 *
 * 这两个接口直接用底层 [HttpClient] 发请求，不经过 `YingdiApiExecutor`：
 * 登录时还没有 token；刷新本身就是执行器遇到 `200002` 时调用的，走执行器会递归。
 */
internal class YingdiAuthRemoteData(private val client: HttpClient, private val json: Json) {
    suspend fun login(username: String, password: String): LoginResponse =
        client.post("$YINGDI_API_BASE_URL/mweb/user/login") {
            setBody(
                YingdiSignedForm.of(
                    "username" to username,
                    "password" to password,
                    "type" to "password",
                    profile = YingdiSignProfile.Mweb,
                ),
            )
        }.decodeYingdiResponse(json)

    suspend fun refreshLogin(refreshToken: String): RefreshLoginResponse =
        client.post("$YINGDI_API_BASE_URL/mweb/user/refresh-login") {
            setBody(YingdiSignedForm.of("refresh_token" to refreshToken, profile = YingdiSignProfile.Mweb))
        }.decodeYingdiResponse(json)
}
