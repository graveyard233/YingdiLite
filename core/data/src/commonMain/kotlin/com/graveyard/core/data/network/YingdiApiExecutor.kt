package com.graveyard.core.data.network

import com.graveyard.core.data.local.preferences.UserSettings
import com.graveyard.core.data.network.sign.YingdiSignedForm
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

internal class YingdiApiExecutor(
    private val client: HttpClient,
    private val json: Json,
    private val settings: UserSettings,
    private val refresher: YingdiTokenRefresher,
) {
    suspend inline fun <reified T> post(
        path: String,
        form: YingdiSignedForm,
        expectedArrayField: String? = null,
        allowNullArray: Boolean = false,
    ): T = post(path, form, serializer<T>(), expectedArrayField, allowNullArray)

    suspend fun <T> post(
        path: String,
        form: YingdiSignedForm,
        serializer: KSerializer<T>,
        expectedArrayField: String? = null,
        allowNullArray: Boolean = false,
    ): T = withRefreshedToken { token ->
        send(path, form, token, serializer, expectedArrayField, allowNullArray)
    }

    /** token 过期（业务错误 200002）时刷新一次并重放；其余错误原样抛出，重放失败也不再刷新。 */
    private suspend fun <T> withRefreshedToken(
        expectedUserId: Long? = null,
        block: suspend (String) -> T,
    ): T {
        val session = settings.session.first()
        if (expectedUserId != null && session?.userId != expectedUserId) {
            throw CancellationException("Authenticated request account changed")
        }
        val token = session?.loginToken ?: "nologin"
        return try {
            block(token)
        } catch (exception: DataException) {
            if (token == "nologin" || (exception.error as? DataError.Business)?.retCode != "200002") {
                throw exception
            }
            if (expectedUserId != null && settings.session.first()?.userId != expectedUserId) {
                throw CancellationException("Authenticated request account changed")
            }
            val refreshed = refresher.refresh(token) ?: throw exception
            if (expectedUserId != null && settings.session.first()?.userId != expectedUserId) {
                throw CancellationException("Authenticated request account changed")
            }
            // 此次发送的错误直接返回，不再进入刷新分支。
            block(refreshed)
        }
    }

    private suspend fun <T> send(
        path: String,
        form: YingdiSignedForm,
        token: String,
        serializer: KSerializer<T>,
        expectedArrayField: String?,
        allowNullArray: Boolean,
    ): T = client.post(YINGDI_API_BASE_URL + path) {
        setBody(form.withLoginToken(token))
    }.decodeYingdiResponse(json, serializer, expectedArrayField, allowNullArray)

    /** 无响应体接口（例如收藏/取消收藏），成功时只校验状态。 */
    suspend fun postNoContent(path: String, form: YingdiSignedForm, expectedUserId: Long? = null): Unit =
        withRefreshedToken(expectedUserId) { token ->
            client.post(YINGDI_API_BASE_URL + path) {
                setBody(form.withLoginToken(token))
            }.verifyYingdiResponse(json)
        }
}
