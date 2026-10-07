package com.graveyard.yingdilite

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import com.graveyard.core.data.di.repositoryModule
import com.graveyard.core.data.local.di.localStorageModule
import com.graveyard.core.data.network.di.networkModule
import com.graveyard.core.designsystem.theme.AppTheme
import com.graveyard.core.utils.logging.Logger
import com.graveyard.feature.news.di.newsModule
import com.graveyard.yingdilite.navigation.AppNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun App(
    dataStore: DataStore<Preferences>,
    onExit: (() -> Unit)? = null,
    enableNetworkDiagnosticBodies: Boolean = false,
) {
    val darkTheme = isSystemInDarkTheme()
    val userSettingsScope = remember {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    DisposableEffect(userSettingsScope) {
        onDispose { userSettingsScope.cancel() }
    }

    // 打开 DEBUG 级日志（含网络诊断），release 包保持 INFO 起步。setDebug 幂等，可随重组执行。
    Logger.setDebug(enableNetworkDiagnosticBodies)

    KoinApplication(
        configuration = koinConfiguration {
            modules(
                networkModule(enableNetworkDiagnosticBodies),
                localStorageModule(dataStore, userSettingsScope),
                repositoryModule,
                newsModule,
            )
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
