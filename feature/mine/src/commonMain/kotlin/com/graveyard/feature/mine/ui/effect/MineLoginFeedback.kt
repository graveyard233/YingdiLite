package com.graveyard.feature.mine.ui.effect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.graveyard.core.data.result.DataError
import com.graveyard.feature.mine.auth.MineLoginCoordinator
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import yingdilite.feature.mine.generated.resources.Res
import yingdilite.feature.mine.generated.resources.mine_login_error_failed
import yingdilite.feature.mine.generated.resources.mine_login_error_http
import yingdilite.feature.mine.generated.resources.mine_login_error_network
import yingdilite.feature.mine.generated.resources.mine_login_error_parsing
import yingdilite.feature.mine.generated.resources.mine_login_error_signature
import yingdilite.feature.mine.generated.resources.mine_login_error_timeout
import yingdilite.feature.mine.generated.resources.mine_login_error_user_not_found

/** Mounted once by the assembly layer, outside the individual login/profile entries. */
@Composable
fun MineLoginFeedback(
    active: Boolean,
    onShowMessage: suspend (String) -> Unit,
) {
    val coordinator = koinInject<MineLoginCoordinator>()
    val showMessage by rememberUpdatedState(onShowMessage)
    LaunchedEffect(coordinator, active) {
        if (!active) return@LaunchedEffect
        // Collect sequentially: consuming a failure must not cancel its snackbar.
        coordinator.state.collect { state ->
            val error = state.error ?: return@collect
            val message = error.loginMessage()
            if (coordinator.consumeFailure(state.attemptId)) showMessage(message)
        }
    }
}

private suspend fun DataError.loginMessage(): String = when (this) {
    DataError.Network -> getString(Res.string.mine_login_error_network)
    DataError.Timeout -> getString(Res.string.mine_login_error_timeout)
    is DataError.Http -> getString(Res.string.mine_login_error_http, statusCode)
    DataError.Parsing -> getString(Res.string.mine_login_error_parsing)
    DataError.Unknown -> getString(Res.string.mine_login_error_failed)
    is DataError.Business -> when (retCode) {
        "100002" -> getString(Res.string.mine_login_error_user_not_found)
        "100009" -> getString(Res.string.mine_login_error_signature)
        else -> retMsg?.takeIf { it.isNotBlank() } ?: getString(Res.string.mine_login_error_failed)
    }
}
