package com.graveyard.core.data.result

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

internal fun Exception.toDataException(): DataException {
    val error = when (this) {
        is CancellationException -> throw this
        is DataException -> return this
        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        is SocketTimeoutException -> DataError.Timeout
        is SerializationException -> DataError.Parsing
        is IOException -> DataError.Network
        else -> DataError.Unknown
    }
    return DataException(error = error, cause = this)
}
