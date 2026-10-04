package com.graveyard.core.data.result

class DataException(
    val error: DataError,
    cause: Throwable? = null,
) : Exception("Data request failed: $error", cause)
