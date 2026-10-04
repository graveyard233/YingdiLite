package com.graveyard.core.data.result

sealed interface DataError {
    data object Network : DataError

    data object Timeout : DataError

    data class Http(val statusCode: Int) : DataError

    data class Business(val retCode: String?, val retMsg: String?) : DataError

    data object Parsing : DataError

    data object Unknown : DataError
}
