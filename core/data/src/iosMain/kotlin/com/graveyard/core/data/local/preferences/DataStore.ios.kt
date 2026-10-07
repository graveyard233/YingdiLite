package com.graveyard.core.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * 由 iOS 入口直接调用（无需 Context），创建全局唯一的 DataStore。
 * 文件放在 App 的 Documents 目录下。
 */
@OptIn(ExperimentalForeignApi::class)
fun createIosDataStore(): DataStore<Preferences> =
    createDataStore {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        requireNotNull(documentDirectory).path + "/$USER_SETTINGS_FILE_NAME"
    }
