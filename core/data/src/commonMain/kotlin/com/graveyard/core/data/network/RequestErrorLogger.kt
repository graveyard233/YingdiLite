package com.graveyard.core.data.network

import com.graveyard.core.utils.logging.Logger

private const val NETWORK_TAG = "YingdiNetwork"

internal fun logNetworkDiagnostic(message: String, isError: Boolean) {
    if (isError) {
        Logger.e(NETWORK_TAG, message)
    } else {
        Logger.d(NETWORK_TAG, message)
    }
}
