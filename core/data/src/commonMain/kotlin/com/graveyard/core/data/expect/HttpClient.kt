package com.graveyard.core.data.expect

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

expect fun getHttpClient(json: Json): HttpClient