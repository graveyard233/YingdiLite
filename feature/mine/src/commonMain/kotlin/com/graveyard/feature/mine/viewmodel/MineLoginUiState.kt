package com.graveyard.feature.mine.viewmodel

internal enum class LoginMethod { Phone, Account }

internal enum class LoginRegion(val dialCode: String, val phoneDigits: Int?) {
    MainlandChina("+86", 11),
    HongKong("+852", 8),
    Macao("+853", 8),
    Taiwan("+886", null),
}

internal enum class LoginFieldError {
    PhoneRequired, PhoneDigits, PhoneLength, PhoneInvalid, AccountRequired, PasswordRequired,
}

internal enum class LoginSubmitResult { Accepted, IdentityInvalid, PasswordInvalid, Unavailable }

internal data class MineLoginUiState(
    val method: LoginMethod,
    val region: LoginRegion,
    val phone: String,
    val account: String,
    val password: String,
    val identityError: LoginFieldError?,
    val passwordError: LoginFieldError?,
    val isSubmitting: Boolean,
    val isRestoring: Boolean,
    val enabled: Boolean,
) {
    override fun toString(): String = "MineLoginUiState(method=$method, isSubmitting=$isSubmitting, credentials=<redacted>)"
}
