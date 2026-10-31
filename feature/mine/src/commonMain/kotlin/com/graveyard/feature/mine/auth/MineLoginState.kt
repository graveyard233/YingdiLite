package com.graveyard.feature.mine.auth

import com.graveyard.core.data.result.DataError

/** Shared submission state observed by the login page, Mine and the feedback effect. */
data class MineLoginState(
    val attemptId: Long = 0,
    val isSubmitting: Boolean = false,
    val error: DataError? = null,
)
