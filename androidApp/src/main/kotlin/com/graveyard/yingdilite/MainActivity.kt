package com.graveyard.yingdilite

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.graveyard.core.data.local.preferences.createAndroidDataStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val dataStore = createAndroidDataStore(applicationContext)
        setContent {
            App(
                dataStore = dataStore,
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
    val context = LocalContext.current.applicationContext
    val dataStore = remember {
        createAndroidDataStore(context)
    }
    App(dataStore = dataStore)
}
