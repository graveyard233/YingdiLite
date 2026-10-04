package com.graveyard.yingdilite

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                onExit = { moveTaskToBack(true) },
                enableNetworkDiagnosticBodies =
                    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
