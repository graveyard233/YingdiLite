package com.graveyard.core.data.network

import com.graveyard.core.data.crypto.YingdiSignPlugin
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Android / iOS 共用的 HttpClient 配置：各端 actual 只负责选引擎。
 *
 * ContentNegotiation 在这里只负责响应反序列化，请求体由 [YingdiSignPlugin] 生成 OutgoingContent。
 */
internal fun HttpClientConfig<*>.configureYingdiClient(
    json: Json,
    enableDiagnosticBodies: Boolean = false,
) {
    install(ContentNegotiation) {
        json(json)
    }
    install(YingdiSignPlugin)
    install(HttpDiagnosticsPlugin) {
        includeBodies = enableDiagnosticBodies
        // Debug 包里成功请求也输出，并在发送前打一行 -->，release 保持错误-only。
        logSuccessfulRequests = enableDiagnosticBodies
        logRequestStart = enableDiagnosticBodies
    }
}
