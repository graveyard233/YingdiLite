package com.graveyard.core.data.network

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.isSaved
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.io.readByteArray
import kotlin.random.Random
import kotlin.time.TimeSource

internal class HttpDiagnosticsConfig {
    var includeBodies: Boolean = false
    var logSuccessfulRequests: Boolean = false

    /** Print a lightweight `--> METHOD url` line before sending, so hung requests stay visible. */
    var logRequestStart: Boolean = false
    var extraSensitiveFields: Set<String> = emptySet()
    var omitBodies: (Url) -> Boolean = { false }
    var logger: (message: String, isError: Boolean) -> Unit = ::logNetworkDiagnostic
}

private class RequestTrace {
    val id: String = Random.nextLong().toULong().toString(16)
    val start = TimeSource.Monotonic.markNow()
    var logged: Boolean = false
}

private val TraceKey = AttributeKey<RequestTrace>("HttpDiagnosticTrace")

/** Logs transformed requests and saved responses without consuming live streams. */
internal val HttpDiagnosticsPlugin = createClientPlugin("HttpDiagnostics", ::HttpDiagnosticsConfig) {
    val config = pluginConfig
    val formatter = HttpDiagnosticFormatter(config.extraSensitiveFields)

    fun bodiesAllowed(url: Url): Boolean = config.includeBodies && try {
        !config.omitBodies(url)
    } catch (_: Exception) {
        false
    }

    fun emit(trace: RequestTrace, isError: Boolean, details: () -> String) {
        if (trace.logged) return
        trace.logged = true
        try {
            // Bound the entire entry as well as each body; sinks must not break requests.
            val message = "[request=${trace.id}] ${details()}".take(14000)
            config.logger(message, isError)
        } catch (_: Exception) {
            // Diagnostics are best effort, never a reason to fail a request.
        }
    }

    on(SendingRequest) { request, _ ->
        val trace = RequestTrace()
        request.attributes.put(TraceKey, trace)
        if (config.logRequestStart) {
            try {
                val url = request.url.build()
                // The start line must not flip trace.logged, otherwise the completion entry is lost.
                config.logger(
                    "[request=${trace.id}] --> ${request.method.value} ${formatter.url(url, bodiesAllowed(url))}",
                    false,
                )
            } catch (_: Exception) {
                // Diagnostics are best effort, never a reason to fail a request.
            }
        }
    }

    on(Send) { request ->
        try {
            proceed(request)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            val trace = request.attributes.getOrNull(TraceKey) ?: RequestTrace()
            emit(trace, true) {
                val url = request.url.build()
                val includeBodies = bodiesAllowed(url)
                buildString {
                    appendLine("<-- FAIL ${request.method.value} ${formatter.url(url, includeBodies)} (${trace.start.elapsedNow().inWholeMilliseconds} ms)")
                    // Exception messages/stack traces can contain raw URLs and credentials.
                    appendLine("Transport failure: ${exception::class.simpleName}")
                    if (includeBodies) {
                        appendLine("Request headers: ${formatter.headers(request.headers.build())}")
                        val content = request.body as? OutgoingContent
                        append("Request body: ${content?.let(formatter::requestBody) ?: "<unavailable>"}")
                    }
                }
            }
            throw exception
        }
    }

    onResponse { response ->
        val failed = !response.status.isSuccess()
        if (!failed && !config.logSuccessfulRequests) return@onResponse
        val request = response.call.request
        val trace = request.attributes.getOrNull(TraceKey) ?: RequestTrace()
        val bodiesAllowed = bodiesAllowed(request.url)
        var responseBody = "<body omitted: disabled>"
        if (bodiesAllowed) {
            responseBody = try {
                when {
                    !response.isSaved -> "<body omitted: streaming response>"
                    !formatter.isStructured(response.contentType()) -> "<body omitted: unsupported content type>"
                    else -> {
                        val bytes = response.bodyAsChannel()
                            .readBuffer(HttpDiagnosticFormatter.MAX_BODY_BYTES.toLong() + 1).readByteArray()
                        if (bytes.size > HttpDiagnosticFormatter.MAX_BODY_BYTES) "<body omitted: too large>"
                        else formatter.body(
                            bytes.decodeToString(throwOnInvalidSequence = true),
                            response.contentType(),
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                "<body omitted: unreadable>"
            }
        }
        emit(trace, failed) {
            buildString {
                appendLine("<-- ${response.status.value} ${request.method.value} ${formatter.url(request.url, bodiesAllowed)} (${trace.start.elapsedNow().inWholeMilliseconds} ms)")
                appendLine("Request Content-Type: ${formatter.line(request.content.contentType?.toString().orEmpty())}")
                appendLine("Response Content-Type: ${formatter.line(response.headers["Content-Type"].orEmpty())}")
                appendLine("Response Request-Id: ${formatter.line(response.headers["Request-Id"].orEmpty())}")
                if (bodiesAllowed) {
                    appendLine("Request headers: ${formatter.headers(request.headers)}")
                    appendLine("Request body: ${formatter.requestBody(request.content)}")
                    appendLine("Response headers: ${formatter.headers(response.headers)}")
                    append("Response body: $responseBody")
                }
            }
        }
    }
}
