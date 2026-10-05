package com.graveyard.yingdilite

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.graveyard.core.data.di.repositoryModule
import com.graveyard.core.data.network.di.networkModule
import com.graveyard.core.designsystem.theme.AppTheme
import com.graveyard.core.utils.logging.Logger
import com.graveyard.feature.news.di.newsModule
import com.graveyard.yingdilite.navigation.AppNavigation
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App(
    onExit: (() -> Unit)? = null,
    enableNetworkDiagnosticBodies: Boolean = false,
) {
    val darkTheme = isSystemInDarkTheme()

    // 打开 DEBUG 级日志（含网络诊断），release 包保持 INFO 起步。setDebug 幂等，可随重组执行。
    Logger.setDebug(enableNetworkDiagnosticBodies)

    KoinApplication(
        configuration = koinConfiguration {
            modules(networkModule(enableNetworkDiagnosticBodies), repositoryModule, newsModule)
        },
    ) {
        AppTheme(darkTheme = darkTheme) {
            AppNavigation(
                darkTheme = darkTheme,
                onExit = onExit,
            )
        }
    }
}
