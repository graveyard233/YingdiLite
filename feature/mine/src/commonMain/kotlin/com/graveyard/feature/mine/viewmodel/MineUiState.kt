package com.graveyard.feature.mine.viewmodel

import com.graveyard.core.data.auth.AuthState

data class MineUiState(
    val authState: AuthState = AuthState.Restoring,
    val isLoggingOut: Boolean = false,
    val isLoggingIn: Boolean = false,
)

enum class MineUiEvent {
    LogoutFailed,
}
