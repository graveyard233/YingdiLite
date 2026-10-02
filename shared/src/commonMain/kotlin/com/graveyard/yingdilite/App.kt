package com.graveyard.yingdilite

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.graveyard.core.designsystem.theme.AppTheme
import com.graveyard.yingdilite.navigation.AppNavigation
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App() {
    val darkTheme = isSystemInDarkTheme()

    KoinApplication(
        configuration = koinConfiguration {
            modules()
        },
    ) {
        AppTheme(darkTheme = darkTheme) {
            AppNavigation(darkTheme = darkTheme)
        }
    }
}
