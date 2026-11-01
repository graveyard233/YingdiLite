package com.graveyard.feature.mine.ui.mine.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect
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

    // 任务在其他页面完成，回到本页时刷新；ViewModel 内部做了节流。
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDailyTasks()
        onPauseOrDispose { }
    }

    MineScreen(
        uiState = uiState,
        onOpenLogin = onOpenLogin,
        onLogout = viewModel::logout,
        onRetryDailyTasks = viewModel::retryDailyTasks,
    )
}
