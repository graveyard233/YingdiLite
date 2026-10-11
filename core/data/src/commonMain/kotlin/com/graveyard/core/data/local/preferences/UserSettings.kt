package com.graveyard.core.data.local.preferences

import com.graveyard.core.model.news.TagInfo
import kotlinx.coroutines.flow.Flow

/**
 * 用户订阅列表和全量标签列表共用的内置默认值，顺序即展示顺序。
 * 未配置、解码失败或持久化值非法时回退使用；id 唯一，title 允许重复。
 * 此值只在读时回退，不会主动写入磁盘。
 */
val DefaultTagInfos: List<TagInfo> = listOf(
    TagInfo(
        id = 859,
        title = "推荐",
        alias = "推荐",
        iconUrl = "https://pic.iyingdi.com/admin/img/58a10a89-1670-4645-ae6f-c57e8f936f87.png",
        fontColor = "#fc5434",
        fontColorDark = "#fc5434",
    ),
    TagInfo(
        id = 17,
        title = "炉石传说",
        alias = "炉石传说",
        iconUrl = "https://pic.iyingdi.com/manage/image/2020/12/12/500772fd-6617-458b-a15d-318f3bbaeb17_w_240_h_240.png",
        fontColor = "#E49739",
        fontColorDark = "#E17D1B",
    ),
    TagInfo(
        id = 23,
        title = "玩家杂谈",
        alias = "玩家杂谈",
        iconUrl = "https://pic.iyingdi.com/manage/image/2020/12/12/97e0b6f7-2829-47a2-b80d-67705dbde8f9_w_240_h_240.png",
        fontColor = "#3AABDA",
        fontColorDark = "#3AABDA",
    ),
)

/**
 * 用户级偏好设置。扁平的小数据走这里；结构化缓存归 Room。
 */
interface UserSettings {

    /** 同一磁盘版本的目录、订阅和派生信息；解码与合并在后台进行。 */
    val tagSettings: Flow<TagSettingsSnapshot>

    /** 语言设置，响应式观察用；未写入时发出默认值。 */
    val language: Flow<String>

    /**
     * 同步获取当前语言，供非 suspend 场景（如 Ktor 签名插件）使用。
     * 后台预热完成前返回默认值，任何平台都不阻塞线程。
     */
    fun getLanguage(): String

    /** 更新语言设置并落盘。 */
    suspend fun setLanguage(value: String)

    /** 用户订阅的新闻标签，顺序即展示顺序；未配置或内容非法时发出 DefaultTagInfos。 */
    val userTagList: Flow<List<TagInfo>>

    /**
     * 同步获取订阅标签，供非 suspend 场景使用。
     * 后台预热完成前或数据损坏时返回内置默认标签，任何平台都不阻塞线程。
     */
    fun getUserTagList(): List<TagInfo>

    /**
     * 覆盖写入订阅标签并落盘。
     *
     * 订阅列表必须非空，且每个标签都必须有正数 ID、非空名称和唯一 ID。
     */
    suspend fun setUserTagList(tags: List<TagInfo>): TagSettingsSnapshot

    /** 全量新闻标签缓存，与用户订阅列表独立；未配置或内容非法时发出 DefaultTagInfos。 */
    val allTagList: Flow<List<TagInfo>>

    /** 同步获取全量标签；后台预热完成前或内容非法时返回 DefaultTagInfos。 */
    fun getAllTagList(): List<TagInfo>

    /**
     * 原子写入远程目录并同步同 ID 的订阅元信息，保留订阅顺序与缺失标签。
     * 列表必须非空，且每个标签都必须有正数 ID、非空白 title 和唯一 ID。
     */
    suspend fun setAllTagList(tags: List<TagInfo>): TagSettingsSnapshot
}

data class TagSettingsSnapshot(
    val userTags: List<TagInfo> = DefaultTagInfos,
    val allTags: List<TagInfo> = DefaultTagInfos,
    val hasCachedCatalog: Boolean = false,
    val hasRemoteCatalog: Boolean = false,
    val revision: Long = 0,
    val missingSubscribedIds: Set<Int> = emptySet(),
    val availableTagRows: List<List<TagInfo>> = emptyList(),
)

/** 调用方必须在后台执行；按 ID 合并，目录缺席不等于已删除。 */
internal fun reconcileTagSettings(
    userTags: List<TagInfo>,
    allTags: List<TagInfo>,
    hasCachedCatalog: Boolean,
    hasRemoteCatalog: Boolean,
    revision: Long,
    previous: TagSettingsSnapshot? = null,
): TagSettingsSnapshot {
    val catalogById = allTags.associateBy { it.id }
    val subscribedIds = userTags.mapTo(HashSet(userTags.size)) { it.id }
    val sameCatalog = previous != null && previous.allTags == allTags
    val sameMembership = previous != null &&
        previous.userTags.mapTo(HashSet(previous.userTags.size)) { it.id } == subscribedIds
    val reconciled = if (hasCachedCatalog) {
        userTags.map { catalogById[it.id] ?: it }
    } else {
        userTags
    }
    return TagSettingsSnapshot(
        userTags = if (reconciled == userTags) userTags else reconciled,
        allTags = if (sameCatalog) checkNotNull(previous).allTags else allTags,
        hasCachedCatalog = hasCachedCatalog,
        hasRemoteCatalog = hasRemoteCatalog,
        revision = revision,
        missingSubscribedIds = if (hasRemoteCatalog) {
            subscribedIds.filterTo(HashSet()) { it !in catalogById }
        } else {
            emptySet()
        },
        availableTagRows = if (sameCatalog && sameMembership) {
            checkNotNull(previous).availableTagRows
        } else {
            allTags.filterNot { it.id in subscribedIds }.chunked(2)
        },
    )
}

internal fun requireValidTagInfos(tags: List<TagInfo>) {
    require(isValidTagInfos(tags)) {
        "Tag infos must be nonempty with positive, unique IDs and nonblank titles."
    }
}

internal fun isValidTagInfos(tags: List<TagInfo>): Boolean {
    if (tags.isEmpty()) return false
    val ids = HashSet<Int>(tags.size)
    return tags.all { it.id > 0 && it.title.isNotBlank() && ids.add(it.id) }
}
