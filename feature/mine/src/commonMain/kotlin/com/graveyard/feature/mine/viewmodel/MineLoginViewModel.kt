package com.graveyard.feature.mine.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.graveyard.core.data.auth.AuthState
import com.graveyard.feature.mine.auth.MineLoginCoordinator
import kotlinx.coroutines.launch

internal class MineLoginViewModel(private val coordinator: MineLoginCoordinator) : ViewModel() {
    // Do not put the password in SavedStateHandle or rememberSaveable.
    var password by mutableStateOf("")
        private set
    var identityError by mutableStateOf<LoginFieldError?>(null)
        private set
    var passwordError by mutableStateOf<LoginFieldError?>(null)
        private set
    val submission = coordinator.state
    val authState = coordinator.authState

    init {
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthState.Authenticated) password = ""
            }
        }
    }

    fun updatePassword(value: String) {
        password = value
        passwordError = null
    }

    fun clearIdentityError() {
        identityError = null
    }

    fun submit(method: LoginMethod, region: LoginRegion, phone: String, account: String): LoginSubmitResult {
        if (submission.value.isSubmitting || authState.value != AuthState.Guest) {
            return LoginSubmitResult.Unavailable
        }
        val normalizedPhone = phone.filterNot { it.isWhitespace() || it == '-' }
        identityError = when (method) {
            LoginMethod.Account -> if (account.trim().isEmpty()) LoginFieldError.AccountRequired else null
            LoginMethod.Phone -> when {
                normalizedPhone.isEmpty() -> LoginFieldError.PhoneRequired
                normalizedPhone.any { it !in '0'..'9' } -> LoginFieldError.PhoneDigits
                region.phoneDigits != null && normalizedPhone.length != region.phoneDigits -> LoginFieldError.PhoneLength
                region == LoginRegion.MainlandChina && !normalizedPhone.startsWith('1') -> LoginFieldError.PhoneInvalid
                region.phoneDigits == null && normalizedPhone.length !in 6..15 -> LoginFieldError.PhoneInvalid
                else -> null
            }
        }
        passwordError = if (password.isEmpty()) LoginFieldError.PasswordRequired else null
        if (identityError != null) return LoginSubmitResult.IdentityInvalid
        if (passwordError != null) return LoginSubmitResult.PasswordInvalid
        val accepted = when (method) {
            LoginMethod.Phone -> coordinator.loginWithPhone(region.dialCode, normalizedPhone, password)
            LoginMethod.Account -> coordinator.loginWithAccount(account.trim(), password)
        }
        return if (accepted) LoginSubmitResult.Accepted else LoginSubmitResult.Unavailable
    }

    override fun onCleared() {
        password = ""
    }
}
