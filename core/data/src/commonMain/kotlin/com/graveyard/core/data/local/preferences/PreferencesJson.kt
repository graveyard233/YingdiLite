package com.graveyard.core.data.local.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.graveyard.core.utils.logging.Logger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Preferences DataStore 只能直接存标量与字符串集合；
 * 自定义对象、任意元素类型的 List 等统一以 JSON 字符串落在 string key 下。
 *
 * 约定：
 * - 模型必须是 `@Serializable`，新增字段一律带默认值，配合 Json 的 ignoreUnknownKeys 完成演进；
 * - 解码失败不抛出，记录日志并按调用方提供的默认值回退，避免读流永久失败；
 * - 需要表达"清除/未设置"时直接对 key 调用 [MutablePreferences.remove]，不要写 null 字符串。
 */

private const val TAG = "PreferencesJson"

/** 将任意可序列化值（含 List<T>）编码为 JSON 后写入。 */
internal inline fun <reified T> MutablePreferences.setJson(
    key: Preferences.Key<String>,
    value: T,
    json: Json,
) {
    this[key] = json.encodeToString(value)
}

/** 读取 JSON 值；key 不存在或解码失败时返回 null（失败会记日志）。 */
internal inline fun <reified T> Preferences.getJsonOrNull(
    key: Preferences.Key<String>,
    json: Json,
): T? {
    val raw = this[key] ?: return null
    return try {
        json.decodeFromString<T>(raw)
    } catch (t: Throwable) {
        Logger.e(TAG, "解码 JSON 偏好失败，回退默认值：${key.name}", t)
        null
    }
}

/** 读取 JSON 值；key 不存在或解码失败时返回 [defaultValue]。 */
internal inline fun <reified T> Preferences.getJson(
    key: Preferences.Key<String>,
    json: Json,
    defaultValue: T,
): T = getJsonOrNull<T>(key, json) ?: defaultValue
