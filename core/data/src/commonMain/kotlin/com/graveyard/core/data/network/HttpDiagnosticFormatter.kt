package com.graveyard.core.data.network

import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.ContentType
import io.ktor.http.ContentDisposition
import io.ktor.http.Headers
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.PartData
import io.ktor.http.content.TextContent
import io.ktor.http.parseQueryString
import io.ktor.util.StringValues
import io.ktor.utils.io.InternalAPI
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class HttpDiagnosticFormatter(
    private val extraSensitiveFields: Set<String> = emptySet(),
    private val maxBodyChars: Int = 4096,
) {
    companion object {
        const val MAX_BODY_BYTES = 65536
        private const val MAX_FIELDS = 64
        private const val MAX_DEPTH = 8
        private const val REDACTED = "<redacted>"
        private val SENSITIVE_NAMES = setOf(
            "sign", "signature", "key", "pwd", "session", "sessionid", "location", "referer",
        )
        private val SENSITIVE_PARTS = listOf(
            "token", "password", "passwd", "secret", "authorization", "cookie", "credential",
        )
    }

    private val sensitiveFields = extraSensitiveFields.map(::normalize).toSet()

    fun isSensitive(name: String): Boolean {
        val key = normalize(name)
        return key in SENSITIVE_NAMES || key.endsWith("apikey") ||
            SENSITIVE_PARTS.any { it in key } || key in sensitiveFields
    }

    fun url(url: Url, includeValues: Boolean): String {
        val base = URLBuilder(url).apply {
            user = null
            password = null
            fragment = ""
            parameters.clear()
        }.buildString()
        val query = fields(url.parameters, hideAll = !includeValues)
        return line(base) + if (query.isEmpty()) "" else "?$query"
    }

    fun headers(headers: Headers): String = fields(headers)

    fun fields(fields: StringValues, hideAll: Boolean = false): String = limit(buildString {
        for ((index, entry) in fields.entries().withIndex()) {
            if (index >= MAX_FIELDS || length >= maxBodyChars) {
                append(" [fields omitted]")
                break
            }
            if (isNotEmpty()) append('&')
            append(line(entry.key))
            append('=')
            if (hideAll || isSensitive(entry.key)) append(REDACTED)
            else append(entry.value.take(MAX_FIELDS).joinToString(",") { line(it) })
        }
    })

    fun isStructured(type: ContentType?): Boolean {
        val mediaType = type?.withoutParameters()?.toString()?.lowercase()
        val charset = type?.parameter("charset")?.lowercase()
        if (charset != null && charset !in setOf("utf-8", "utf8")) return false
        return mediaType == "application/x-www-form-urlencoded" ||
            mediaType == "application/json" || mediaType?.endsWith("+json") == true
    }

    fun body(text: String, type: ContentType?): String {
        if (text.isEmpty()) return "<empty>"
        if (text.length > MAX_BODY_BYTES) return "<body omitted: too large>"
        if (!isStructured(type)) return "<body omitted: unsupported content type>"
        return try {
            val formatted = if (type?.match(ContentType.Application.FormUrlEncoded) == true) {
                fields(parseQueryString(text))
            } else {
                sanitizeJson(Json.parseToJsonElement(text), 0).toString()
            }
            limit(formatted)
        } catch (_: Exception) {
            // Never fall back to raw text when structured redaction fails.
            "<body omitted: invalid structured content>"
        }
    }

    @OptIn(InternalAPI::class)
    fun requestBody(content: OutgoingContent): String = try {
        formatRequestBody(content)
    } catch (_: Exception) {
        "<body omitted: unreadable>"
    }

    @OptIn(InternalAPI::class)
    private fun formatRequestBody(content: OutgoingContent): String = when (content) {
        is FormDataContent -> fields(content.formData)
        is TextContent -> body(content.text, content.contentType)
        is MultiPartFormDataContent -> limit(
            content.parts.take(MAX_FIELDS).joinToString("; ") { part ->
                val name = part.name.orEmpty()
                val metadata = if (isSensitive(name)) REDACTED
                else if (part is PartData.FormItem) "<form field; value omitted>"
                else {
                    val disposition = part.headers.getAll("Content-Disposition").orEmpty()
                        .joinToString("; ")
                    val filename = ContentDisposition.parse(disposition).parameter("filename")
                    "file=${line(filename.orEmpty())}, " +
                        "type=${line(part.contentType?.toString().orEmpty())}, " +
                        "size=${line(part.headers["Content-Length"] ?: "unknown")}; content omitted"
                }
                "${line(name)}: $metadata"
            },
        )
        is OutgoingContent.ByteArrayContent -> {
            if (!isStructured(content.contentType)) "<body omitted: binary content>"
            else if ((content.contentLength ?: 0) > MAX_BODY_BYTES) "<body omitted: too large>"
            else {
                val bytes = content.bytes()
                if (bytes.size > MAX_BODY_BYTES) "<body omitted: too large>"
                else body(bytes.decodeToString(throwOnInvalidSequence = true), content.contentType)
            }
        }
        is OutgoingContent.NoContent -> "<empty>"
        else -> "<body omitted: streaming content>"
    }

    private fun sanitizeJson(element: JsonElement, depth: Int): JsonElement {
        if (depth >= MAX_DEPTH) return JsonPrimitive("<nested content omitted>")
        return when (element) {
            is JsonObject -> JsonObject(
                element.entries.take(MAX_FIELDS).associate { (name, value) ->
                    name to if (isSensitive(name)) JsonPrimitive(REDACTED)
                    else sanitizeJson(value, depth + 1)
                } + if (element.size > MAX_FIELDS) {
                    mapOf("_diagnostic_truncated" to JsonPrimitive(true))
                } else emptyMap(),
            )
            is JsonArray -> JsonArray(
                element.take(MAX_FIELDS).map { sanitizeJson(it, depth + 1) } +
                    if (element.size > MAX_FIELDS) listOf(JsonPrimitive("<items omitted>")) else emptyList(),
            )
            else -> element
        }
    }

    fun line(value: String): String = value.replace("\r", "\\r").replace("\n", "\\n")
        .take(maxBodyChars)

    private fun limit(value: String): String =
        if (value.length <= maxBodyChars) value else value.take(maxBodyChars) + " [truncated]"

    private fun normalize(value: String): String = value.lowercase().filter { it.isLetterOrDigit() }
}
