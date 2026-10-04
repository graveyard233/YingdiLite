package com.graveyard.core.data.result

sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>

    data class Failure(val error: DataError) : DataResult<Nothing>
}
