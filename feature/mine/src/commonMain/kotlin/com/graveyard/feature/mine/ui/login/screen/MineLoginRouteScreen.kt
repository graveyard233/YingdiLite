package com.graveyard.feature.mine.ui.login.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.graveyard.core.data.auth.AuthState
import com.graveyard.feature.mine.model.LegalAgreement
import com.graveyard.feature.mine.viewmodel.LoginMethod
import com.graveyard.feature.mine.viewmodel.LoginRegion
import com.graveyard.feature.mine.viewmodel.LoginSubmitResult
import com.graveyard.feature.mine.viewmodel.MineLoginUiState
import com.graveyard.feature.mine.viewmodel.MineLoginViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MineLoginRouteScreen(
    onBack: () -> Unit,
    onOpenAgreement: (LegalAgreement) -> Unit,
    interactive: Boolean,
) {
    val viewModel = koinViewModel<MineLoginViewModel>()
    val submission by viewModel.submission.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var method by rememberSaveable { mutableStateOf(LoginMethod.Phone) }
    var region by rememberSaveable { mutableStateOf(LoginRegion.MainlandChina) }
    var phone by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    val enabled = interactive && authState == AuthState.Guest && !submission.isSubmitting

    MineLoginScreen(
        uiState = MineLoginUiState(
            method = method,
            region = region,
            phone = phone,
            account = account,
            password = viewModel.password,
            identityError = viewModel.identityError,
            passwordError = viewModel.passwordError,
            isSubmitting = submission.isSubmitting,
            isRestoring = authState == AuthState.Restoring,
            enabled = enabled,
        ),
        onBack = onBack,
        onOpenAgreement = onOpenAgreement,
        onMethodSelected = {
            if (enabled) {
                method = it
                viewModel.clearIdentityError()
            }
        },
        onRegionSelected = {
            if (enabled) {
                region = it
                viewModel.clearIdentityError()
            }
        },
        onPhoneChanged = {
            if (enabled) {
                phone = it
                viewModel.clearIdentityError()
            }
        },
        onAccountChanged = {
            if (enabled) {
                account = it
                viewModel.clearIdentityError()
            }
        },
        onPasswordChanged = { if (enabled) viewModel.updatePassword(it) },
        onSubmit = {
            if (enabled) viewModel.submit(method, region, phone, account) else LoginSubmitResult.Unavailable
        },
    )
}
