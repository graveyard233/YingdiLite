package com.graveyard.core.data.network

import android.util.Log

internal actual fun logNetworkDiagnostic(message: String, isError: Boolean) {
    val priority = if (isError) Log.ERROR else Log.DEBUG
    // One line per entry preserves the request id and avoids Logcat message truncation.
    val requestId = message.substringBefore(']')
    message.substringAfter("] ").lineSequence().forEach { line ->
        line.chunked(900).forEach { chunk ->
            Log.println(priority, "YingdiNetwork", "$requestId] $chunk")
        }
    }
}
