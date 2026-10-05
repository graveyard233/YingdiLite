package com.graveyard.core.data.paging

import com.graveyard.core.data.result.DataError

/** Identity also identifies a request that finishes between two UI frames. */
class NewsRefreshRequest internal constructor()

sealed interface NewsRefreshEvent {
    val request: NewsRefreshRequest

    data class Started(override val request: NewsRefreshRequest) : NewsRefreshEvent

    data class Completed(
        override val request: NewsRefreshRequest,
        val error: DataError? = null,
    ) : NewsRefreshEvent

    data class Cancelled(override val request: NewsRefreshRequest) : NewsRefreshEvent
}
