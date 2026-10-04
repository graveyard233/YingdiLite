package com.graveyard.core.data.expect

import com.graveyard.core.data.network.configureYingdiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json

actual fun getHttpClient(json: Json, enableDiagnosticBodies: Boolean): HttpClient {
    return HttpClient(OkHttp) {
        configureYingdiClient(json, enableDiagnosticBodies)
    }
}
