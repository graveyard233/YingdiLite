package com.graveyard.yingdilite

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.graveyard.core.data.di.repositoryModule
import com.graveyard.core.data.network.di.networkModule
import com.graveyard.core.designsystem.theme.AppTheme
import com.graveyard.feature.news.di.newsModule
import com.graveyard.yingdilite.navigation.AppNavigation
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App(
    onExit: (() -> Unit)? = null,
) {
    val darkTheme = isSystemInDarkTheme()

    KoinApplication(
        configuration = koinConfiguration {
            modules(networkModule, repositoryModule, newsModule)
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
