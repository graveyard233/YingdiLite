package com.graveyard.feature.mine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.graveyard.core.data.repository.AuthRepository
import com.graveyard.core.data.result.DataResult
import com.graveyard.feature.mine.auth.MineLoginCoordinator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MineViewModel(
    private val authRepository: AuthRepository,
    loginCoordinator: MineLoginCoordinator,
) : ViewModel() {
    private val isLoggingOut = MutableStateFlow(false)
    private val eventChannel = Channel<MineUiEvent>(Channel.BUFFERED)

    val uiState = combine(authRepository.state, isLoggingOut, loginCoordinator.state) { authState, loggingOut, login ->
        MineUiState(authState = authState, isLoggingOut = loggingOut, isLoggingIn = login.isSubmitting)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MineUiState(
            authState = authRepository.state.value,
            isLoggingIn = loginCoordinator.state.value.isSubmitting,
        ),
    )
    val events = eventChannel.receiveAsFlow()

    fun logout() {
        if (isLoggingOut.value) return
        isLoggingOut.value = true
        viewModelScope.launch {
            try {
                when (authRepository.logout()) {
                    is DataResult.Success -> Unit
                    is DataResult.Failure -> eventChannel.send(MineUiEvent.LogoutFailed)
                }
            } finally {
                isLoggingOut.value = false
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
