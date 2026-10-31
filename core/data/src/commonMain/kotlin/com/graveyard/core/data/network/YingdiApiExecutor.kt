package com.graveyard.core.data.network

import com.graveyard.core.data.local.preferences.UserSettings
import com.graveyard.core.data.network.sign.YingdiSignedForm
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
    ): T {
        val token = settings.session.first()?.loginToken ?: "nologin"
        return try {
            send(path, form, token, serializer, expectedArrayField, allowNullArray)
        } catch (exception: DataException) {
            if (token == "nologin" || (exception.error as? DataError.Business)?.retCode != "200002") {
                throw exception
            }
            val refreshed = refresher.refresh(token) ?: throw exception
            // 此次发送的错误直接返回，不再进入刷新分支。
            send(path, form, refreshed, serializer, expectedArrayField, allowNullArray)
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
}
