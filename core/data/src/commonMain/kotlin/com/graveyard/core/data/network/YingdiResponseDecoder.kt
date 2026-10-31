package com.graveyard.core.data.network

import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.serializer

internal const val YINGDI_API_BASE_URL = "https://api.iyingdi.com"

internal suspend inline fun <reified T> HttpResponse.decodeYingdiResponse(
    json: Json,
    expectedArrayField: String? = null,
    allowNullArray: Boolean = false,
): T = decodeYingdiResponse(json, serializer<T>(), expectedArrayField, allowNullArray)

/** 先识别业务错误信封，再检查 HTTP 状态；成功响应保留原有结构校验。 */
internal suspend fun <T> HttpResponse.decodeYingdiResponse(
    json: Json,
    serializer: KSerializer<T>,
    expectedArrayField: String? = null,
    allowNullArray: Boolean = false,
): T {
    val body = bodyAsText()
    currentCoroutineContext().ensureActive()
    return withContext(Dispatchers.Default) {
        val payload = try {
            json.parseToJsonElement(body)
        } catch (exception: SerializationException) {
            if (!status.isSuccess()) throw DataException(DataError.Http(status.value))
            throw exception
        }
        if (payload is JsonObject && ("retCode" in payload || "retMsg" in payload)) {
            throw DataException(
                DataError.Business(
                    (payload["retCode"] as? JsonPrimitive)?.contentOrNull,
                    (payload["retMsg"] as? JsonPrimitive)?.contentOrNull,
                ),
            )
        }
        if (!status.isSuccess()) throw DataException(DataError.Http(status.value))
        when (payload) {
            is JsonObject -> if (expectedArrayField != null) {
                val field = payload[expectedArrayField]
                if (field !is JsonArray && !(allowNullArray && field is JsonNull)) {
                    throw DataException(DataError.Parsing)
                }
            }
            is JsonArray -> if (expectedArrayField != null) throw DataException(DataError.Parsing)
            else -> throw DataException(DataError.Parsing)
        }
        currentCoroutineContext().ensureActive()
        json.decodeFromJsonElement(serializer, payload)
    }
}
