package com.graveyard.core.data.local.preferences

import com.graveyard.core.data.network.model.NewsArticleTag
import kotlinx.coroutines.flow.Flow

/**
 * 内置默认新闻标签：未配置或持久化值非法时回退使用，顺序即展示顺序。
 * id 唯一；label 允许重复。此值只在读时回退，不会主动写入磁盘。
 */
val DefaultArticleTags: List<NewsArticleTag> = listOf(
    NewsArticleTag(label = "炉石传说", id = 17),
    NewsArticleTag(label = "推荐", id = 859),
    NewsArticleTag(label = "万智牌", id = 18),
)

/**
 * 用户级偏好设置。扁平的小数据走这里；结构化缓存归 Room。
 */
interface UserSettings {

    /** 语言设置，响应式观察用；未写入时发出默认值。 */
    val language: Flow<String>

    /**
     * 同步获取当前语言，供非 suspend 场景（如 Ktor 签名插件）使用。
     * 后台预热完成前返回默认值，任何平台都不阻塞线程。
     */
    fun getLanguage(): String

    /** 更新语言设置并落盘。 */
    suspend fun setLanguage(value: String)

    /** 订阅的新闻标签，响应式观察用；从未写入时发出内置默认标签。 */
    val articleTags: Flow<List<NewsArticleTag>>

    /**
     * 同步获取订阅标签，供非 suspend 场景使用。
     * 后台预热完成前或数据损坏时返回内置默认标签，任何平台都不阻塞线程。
     */
    fun getArticleTags(): List<NewsArticleTag>

    /**
     * 覆盖写入订阅标签并落盘。
     *
     * 订阅列表必须非空，且每个标签都必须有正数 ID、非空名称和唯一 ID。
     */
    suspend fun setArticleTags(tags: List<NewsArticleTag>)
}

internal fun requireValidArticleTags(tags: List<NewsArticleTag>) {
    require(tags.isNotEmpty()) { "Article tags must not be empty." }
    require(tags.all { it.id > 0 }) { "Article tag IDs must be positive." }
    require(tags.all { it.label.isNotBlank() }) { "Article tag labels must not be blank." }
    require(tags.map { it.id }.distinct().size == tags.size) {
        "Article tag IDs must be unique."
    }
}

internal fun isValidArticleTags(tags: List<NewsArticleTag>): Boolean =
    tags.isNotEmpty() &&
        tags.all { it.id > 0 && it.label.isNotBlank() } &&
        tags.map { it.id }.distinct().size == tags.size
