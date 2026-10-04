package com.graveyard.core.data.network

internal actual fun logNetworkDiagnostic(message: String, isError: Boolean) {
    println("YingdiNetwork ${if (isError) "ERROR" else "DEBUG"}: $message")
}
