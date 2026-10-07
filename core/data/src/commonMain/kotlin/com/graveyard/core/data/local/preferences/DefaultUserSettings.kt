package com.graveyard.core.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.graveyard.core.data.network.model.NewsArticleTag
import com.graveyard.core.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

internal class DefaultUserSettings(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    private val scope: CoroutineScope,
) : UserSettings {

    // 整份偏好的最近一次快照；null 表示磁盘值尚未加载完成，同步读取此时回退默认值。
    private val cached = MutableStateFlow<Preferences?>(null)
    private val writeMutex = Mutex()

    init {
        scope.launch {
            // 兜底：读盘异常（路径/权限/未知损坏）不能让后台协程带着未捕获异常杀掉进程；
            // 此时缓存保持 null，所有同步 get 继续回退默认值。
            try {
                dataStore.data.collect { cached.value = it }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Logger.e(TAG, "预热用户设置失败，同步读取将回退默认值", t)
            }
        }
    }

    override val language: Flow<String> =
        dataStore.data.map { preferences -> preferences[LANGUAGE_KEY] ?: DEFAULT_LANGUAGE }

    override fun getLanguage(): String =
        cached.value?.get(LANGUAGE_KEY) ?: DEFAULT_LANGUAGE

    override suspend fun setLanguage(value: String) {
        writeMutex.withLock {
            cached.value = dataStore.edit { preferences ->
                preferences[LANGUAGE_KEY] = value
            }
        }
    }

    override val articleTags: Flow<List<NewsArticleTag>> =
        dataStore.data.map { preferences ->
            preferences.getArticleTagsOrDefault()
        }

    override fun getArticleTags(): List<NewsArticleTag> =
        cached.value?.getArticleTagsOrDefault()
            ?: DefaultArticleTags

    override suspend fun setArticleTags(tags: List<NewsArticleTag>) {
        requireValidArticleTags(tags)
        writeMutex.withLock {
            cached.value = dataStore.edit { preferences ->
                preferences.setJson(ARTICLE_TAGS_KEY, tags, json)
            }
        }
    }

    private fun Preferences.getArticleTagsOrDefault(): List<NewsArticleTag> {
        val tags = getJsonOrNull<List<NewsArticleTag>>(ARTICLE_TAGS_KEY, json)
        return when {
            tags == null -> DefaultArticleTags
            isValidArticleTags(tags) -> tags
            else -> {
                Logger.e(TAG, "用户标签偏好不合法，回退默认标签")
                DefaultArticleTags
            }
        }
    }

    private companion object {
        const val TAG = "UserSettings"
        const val DEFAULT_LANGUAGE = "zh"
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val ARTICLE_TAGS_KEY = stringPreferencesKey("article_tags")
    }
}
