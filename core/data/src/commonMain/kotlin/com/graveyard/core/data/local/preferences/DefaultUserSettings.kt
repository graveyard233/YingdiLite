package com.graveyard.core.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.graveyard.core.model.news.TagInfo
import com.graveyard.core.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal class DefaultUserSettings(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    private val scope: CoroutineScope,
) : UserSettings {

    // 整份偏好的最近一次快照；null 表示磁盘值尚未加载完成，同步读取此时回退默认值。
    private val cached = MutableStateFlow<Preferences?>(null)
    private val cachedTags = MutableStateFlow<TagSettingsSnapshot?>(null)
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
            .distinctUntilChanged()

    override fun getLanguage(): String =
        cached.value?.get(LANGUAGE_KEY) ?: DEFAULT_LANGUAGE

    override suspend fun setLanguage(value: String) {
        writeMutex.withLock {
            cached.value = dataStore.edit { preferences ->
                preferences[LANGUAGE_KEY] = value
            }
        }
    }

    private val tagResults = dataStore.data
        .map { it.storedTags() }
        // 语言等无关偏好变化不重复解码、校验或分组。
        .distinctUntilChanged()
        .map { stored ->
            Result.success(stored.decode(cachedTags.value).also(::cacheSnapshot))
        }
        .retryWhen { cause, _ ->
            if (cause is CancellationException) throw cause
            Logger.e(TAG, "读取标签设置失败，保留最近一次快照", cause)
            emit(Result.failure(cause))
            delay(1_000L)
            true
        }
        .flowOn(Dispatchers.Default)
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    override val tagSettings: Flow<TagSettingsSnapshot> = tagResults.map { it.getOrThrow() }

    override val userTagList: Flow<List<TagInfo>> =
        tagResults.map { it.getOrNull()?.userTags ?: getUserTagList() }
            .distinctUntilChanged().flowOn(Dispatchers.Default)

    override fun getUserTagList(): List<TagInfo> = cachedTags.value?.userTags ?: DefaultTagInfos

    override val allTagList: Flow<List<TagInfo>> =
        tagResults.map { it.getOrNull()?.allTags ?: getAllTagList() }
            .distinctUntilChanged().flowOn(Dispatchers.Default)

    override fun getAllTagList(): List<TagInfo> = cachedTags.value?.allTags ?: DefaultTagInfos

    override suspend fun setUserTagList(tags: List<TagInfo>): TagSettingsSnapshot =
        writeTags(tags, replaceCatalog = false)

    override suspend fun setAllTagList(tags: List<TagInfo>): TagSettingsSnapshot =
        writeTags(tags, replaceCatalog = true)

    private suspend fun writeTags(
        tags: List<TagInfo>,
        replaceCatalog: Boolean,
    ): TagSettingsSnapshot = withContext(Dispatchers.Default) {
        requireValidTagInfos(tags)
        writeMutex.withLock {
            var committed: TagSettingsSnapshot? = null
            dataStore.edit { preferences ->
                // 在事务内读取最新订阅，避免网络请求期间的增删/排序被旧快照覆盖。
                // 包括 JSON 编解码在内的事务变换也显式放到后台。
                withContext(Dispatchers.Default) {
                    val previous = preferences.storedTags().decode(cachedTags.value)
                    val snapshot = reconcileTagSettings(
                        userTags = if (replaceCatalog) previous.userTags else tags,
                        allTags = if (replaceCatalog) tags else previous.allTags,
                        hasCachedCatalog = replaceCatalog || previous.hasCachedCatalog,
                        hasRemoteCatalog = replaceCatalog || previous.hasRemoteCatalog,
                        revision = previous.revision + 1,
                        previous = previous,
                    )
                    preferences.setJson(TAGS_USER_KEY, snapshot.userTags, json)
                    if (replaceCatalog) {
                        preferences.setJson(TAGS_ALL_KEY, snapshot.allTags, json)
                        preferences[TAGS_REMOTE_KEY] = true
                    }
                    preferences[TAGS_REVISION_KEY] = snapshot.revision
                    committed = snapshot
                }
            }
            // edit 成功落盘后才返回，失败不改变编辑器的回滚基线。
            checkNotNull(committed).also(::cacheSnapshot)
        }
    }

    private fun cacheSnapshot(snapshot: TagSettingsSnapshot) {
        cachedTags.update { previous ->
            if (previous == null || snapshot.revision >= previous.revision) snapshot else previous
        }
    }

    private fun Preferences.storedTags() = StoredTags(
        user = this[TAGS_USER_KEY],
        all = this[TAGS_ALL_KEY],
        remote = this[TAGS_REMOTE_KEY] == true,
        revision = this[TAGS_REVISION_KEY] ?: 0,
    )

    private fun StoredTags.decode(previous: TagSettingsSnapshot? = null): TagSettingsSnapshot {
        fun decodeList(raw: String?): List<TagInfo>? {
            if (raw == null) return null
            return try {
                json.decodeFromString<List<TagInfo>>(raw).takeIf { tags ->
                    isValidTagInfos(tags).also { valid ->
                        if (!valid) Logger.e(TAG, "标签偏好不合法，回退默认标签")
                    }
                }
            } catch (exception: Exception) {
                Logger.e(TAG, "标签偏好解码失败，回退默认标签", exception)
                null
            }
        }
        val userTags = decodeList(user) ?: DefaultTagInfos
        val catalog = decodeList(all)
        // 兼容旧缓存；未标记来源的默认列表仍需要自动刷新。
        val hasCache = catalog != null && (remote || catalog != DefaultTagInfos)
        return reconcileTagSettings(
            userTags = userTags,
            allTags = catalog ?: DefaultTagInfos,
            hasCachedCatalog = hasCache,
            hasRemoteCatalog = remote && catalog != null,
            revision = revision,
            previous = previous,
        )
    }

    private data class StoredTags(
        val user: String?,
        val all: String?,
        val remote: Boolean,
        val revision: Long,
    )

    private companion object {
        const val TAG = "UserSettings"
        const val DEFAULT_LANGUAGE = "zh"
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val TAGS_USER_KEY = stringPreferencesKey("tags_user_tags")
        val TAGS_ALL_KEY = stringPreferencesKey("tags_all_tags")
        val TAGS_REMOTE_KEY = booleanPreferencesKey("tags_catalog_remote")
        val TAGS_REVISION_KEY = longPreferencesKey("tags_revision")
    }
}
