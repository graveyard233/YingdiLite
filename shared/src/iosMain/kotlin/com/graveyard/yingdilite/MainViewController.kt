package com.graveyard.yingdilite

import androidx.compose.ui.window.ComposeUIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
fun MainViewController() = ComposeUIViewController {
    App(onExit = null, enableNetworkDiagnosticBodies = Platform.isDebugBinary)
}
