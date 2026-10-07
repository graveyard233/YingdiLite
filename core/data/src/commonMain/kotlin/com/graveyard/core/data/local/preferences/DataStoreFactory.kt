package com.graveyard.core.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import okio.Path.Companion.toPath

/**
 * 全应用唯一的 Preferences 存储文件名。
 * 后缀 .preferences_pb 是 Android 端 PreferenceDataStoreFactory 的强制要求（iOS 不校验），
 * 仅表示底层编码格式，与 Proto DataStore/.proto 文件无关，key 仍全部在 Kotlin 中定义。
 */
const val USER_SETTINGS_FILE_NAME = "user_settings.preferences_pb"

/**
 * 创建单例 Preferences DataStore；平台差异只剩"给出文件路径"。
 *
 * 同一文件严禁创建多个实例，调用方通过 Koin single 管理生命周期。
 * 文件损坏时重置为空偏好，避免读流程持续抛 CorruptionException。
 */
internal fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { producePath().toPath() },
    )
