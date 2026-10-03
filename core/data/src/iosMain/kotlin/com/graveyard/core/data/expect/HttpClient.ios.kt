package com.graveyard.core.data.expect

import com.graveyard.core.data.network.configureYingdiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.serialization.json.Json

actual fun getHttpClient(json: Json): HttpClient {
    return HttpClient(Darwin) {
        configureYingdiClient(json)
    }
}
