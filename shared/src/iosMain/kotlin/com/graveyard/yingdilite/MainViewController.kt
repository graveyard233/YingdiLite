package com.graveyard.yingdilite

import androidx.compose.ui.window.ComposeUIViewController
import com.graveyard.core.data.local.preferences.createIosDataStore
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
fun MainViewController() = run {
    val dataStore = createIosDataStore()
    ComposeUIViewController {
        App(
            dataStore = dataStore,
            onExit = null,
            enableNetworkDiagnosticBodies = Platform.isDebugBinary,
        )
    }
}
