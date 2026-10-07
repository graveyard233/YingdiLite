package com.graveyard.core.data.local.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.graveyard.core.data.local.preferences.DefaultUserSettings
import com.graveyard.core.data.local.preferences.UserSettings
import kotlinx.coroutines.CoroutineScope
import org.koin.dsl.module

/**
 * 本地存储模块。DataStore 实例由平台入口创建后注入（同一文件只允许一个实例）。
 *
 * UserSettings 标记 createdAtStart：Koin 启动即创建并开始后台预热语言缓存，
 * 保证非 suspend 调用方（如网络签名插件）尽早能取到磁盘值。
 */
fun localStorageModule(
    dataStore: DataStore<Preferences>,
    userSettingsScope: CoroutineScope,
) = module {
    single { dataStore }
    single<UserSettings>(createdAtStart = true) {
        // Json 单例由 networkModule 提供（ignoreUnknownKeys，兼容存储字段演进）。
        DefaultUserSettings(
            dataStore = get(),
            json = get(),
            scope = userSettingsScope,
        )
    }
}
