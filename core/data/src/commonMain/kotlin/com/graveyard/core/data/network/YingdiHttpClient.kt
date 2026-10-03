package com.graveyard.core.data.network

import com.graveyard.core.data.crypto.YingdiSignPlugin
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Android / iOS 共用的 HttpClient 配置：各端 actual 只负责选引擎。
 *
 * ContentNegotiation 在这里只负责响应反序列化，请求体由 [YingdiSignPlugin] 生成 OutgoingContent。
 */
internal fun HttpClientConfig<*>.configureYingdiClient(json: Json) {
    install(ContentNegotiation) {
        json(json)
    }
    install(Logging) {
        level = LogLevel.INFO
    }
    install(YingdiSignPlugin)
}
