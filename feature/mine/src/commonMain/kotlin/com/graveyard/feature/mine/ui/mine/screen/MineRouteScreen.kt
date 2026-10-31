package com.graveyard.feature.mine.ui.mine.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.graveyard.feature.mine.viewmodel.MineUiEvent
import com.graveyard.feature.mine.viewmodel.MineViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yingdilite.feature.mine.generated.resources.Res
import yingdilite.feature.mine.generated.resources.mine_logout_failed

@Composable
fun MineRouteScreen(
    onOpenLogin: () -> Unit,
    onShowMessage: suspend (String) -> Unit,
    viewModel: MineViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val logoutFailedMessage = stringResource(Res.string.mine_logout_failed)
    val showMessage by rememberUpdatedState(onShowMessage)

    LaunchedEffect(viewModel, logoutFailedMessage) {
        viewModel.events.collect { event ->
            when (event) {
                MineUiEvent.LogoutFailed -> showMessage(logoutFailedMessage)
            }
        }
    }

    MineScreen(
        uiState = uiState,
        onOpenLogin = onOpenLogin,
        onLogout = viewModel::logout,
    )
}
