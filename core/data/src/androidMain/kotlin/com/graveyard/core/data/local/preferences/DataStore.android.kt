package com.graveyard.core.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * 由 Android 入口（Application/Activity Context）调用，创建全局唯一的 DataStore。
 * 路径对齐 AndroidX `context.dataStoreFile()` 的惯例：filesDir/datastore/<name>。
 */
fun createAndroidDataStore(context: Context): DataStore<Preferences> =
    createDataStore {
        context.filesDir.resolve("datastore/$USER_SETTINGS_FILE_NAME").absolutePath
    }
