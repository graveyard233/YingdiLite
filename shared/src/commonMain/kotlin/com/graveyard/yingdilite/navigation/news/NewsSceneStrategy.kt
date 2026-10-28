package com.graveyard.yingdilite.navigation.news

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.graveyard.feature.news.navigation.ArticleRoute
import com.graveyard.feature.news.navigation.ArticleRepliesRoute
import com.graveyard.feature.news.navigation.LocalNewsPresentation
import com.graveyard.feature.news.navigation.NewsLayoutMode
import com.graveyard.feature.news.navigation.NewsPresentation
import com.graveyard.feature.news.navigation.NewsRoute
import com.graveyard.yingdilite.navigation.APP_PRESENTATION_KEY
import com.graveyard.yingdilite.navigation.APP_ROUTE_KEY
import com.graveyard.yingdilite.navigation.APP_TOP_LEVEL_KEY
import com.graveyard.yingdilite.navigation.AdaptivePaneLayout
import com.graveyard.yingdilite.navigation.AppScenePresentation

private const val NEWS_READING_WORKSPACE_KEY = "news-reading-workspace"
// 新闻布局模式仅供本功能的场景切换判断使用，避免应用公共状态依赖 NewsLayoutMode。
private const val NEWS_LAYOUT_MODE_KEY = "com.graveyard.yingdilite.news.layoutMode"

/** 宽屏下文章与回复之间只替换辅助 pane，保留阅读区域，跳过整个场景的滑动动画。 */
internal fun isRepliesPaneChange(initial: Scene<NavKey>, target: Scene<NavKey>): Boolean {
    val from = initial.entries.lastOrNull()?.metadata?.get(APP_ROUTE_KEY)
    val to = target.entries.lastOrNull()?.metadata?.get(APP_ROUTE_KEY)
    val sameArticle = (from is ArticleRepliesRoute && from.article == to) ||
        (to is ArticleRepliesRoute && to.article == from)
    return sameArticle && (initial.metadata[NEWS_LAYOUT_MODE_KEY] as? NewsLayoutMode) in sideBySideModes &&
        (target.metadata[NEWS_LAYOUT_MODE_KEY] as? NewsLayoutMode) in sideBySideModes
}

private val sideBySideModes = setOf(NewsLayoutMode.ReadingDual, NewsLayoutMode.ReadingTriple)

internal data class NewsSceneStrategy(
    val feedLayout: NewsLayout,
    val readingLayout: NewsLayout,
) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? =
        createScene(entries)

    fun createScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        val last = entries.lastOrNull() ?: return null
        if (last.metadata[APP_TOP_LEVEL_KEY] != NewsRoute) return null
        val route = last.metadata[APP_ROUTE_KEY]
        if (route == NewsRoute) {
            return NewsScene(feedLayout, last, null, emptyList())
        }
        if (route !is ArticleRoute && route !is ArticleRepliesRoute) return null
        // 只组合当前新闻 Tab 中连续的列表、文章和回复条目，避免混入其他导航流程。
        val feedIndex = entries.indexOfLast { it.metadata[APP_ROUTE_KEY] == NewsRoute }
        if (feedIndex < 0 || entries.subList(feedIndex, entries.size).any {
                it.metadata[APP_TOP_LEVEL_KEY] != NewsRoute ||
                    (it.metadata[APP_ROUTE_KEY] != NewsRoute && it.metadata[APP_ROUTE_KEY] !is ArticleRoute &&
                        it.metadata[APP_ROUTE_KEY] !is ArticleRepliesRoute)
            }) return null
        val articleEntry = if (route is ArticleRepliesRoute) {
            entries.getOrNull(entries.lastIndex - 1)?.takeIf { it.metadata[APP_ROUTE_KEY] == route.article }
                ?: return null
        } else last
        return NewsScene(readingLayout, entries[feedIndex], articleEntry, entries.dropLast(1),
            repliesEntry = last.takeIf { route is ArticleRepliesRoute })
    }
}

internal data class NewsScene(
    val layout: NewsLayout,
    val feedEntry: NavEntry<NavKey>,
    val articleEntry: NavEntry<NavKey>?,
    override val previousEntries: List<NavEntry<NavKey>>,
    val repliesEntry: NavEntry<NavKey>? = null,
) : Scene<NavKey> {
    private val selected = articleEntry?.metadata?.get(APP_ROUTE_KEY) as? ArticleRoute

    // 列表选文复用阅读场景，文章内链接则使用独立实例触发导航过渡。
    // 场景标识不依赖布局模式，窗口缩放时仍能沿用原有身份。
    override val key: Any = repliesEntry?.contentKey ?: when (layout.mode) {
        NewsLayoutMode.Feed -> "news-feed"
        else -> selected?.instanceId?.takeIf { it.isNotBlank() }
            ?.let { NEWS_READING_WORKSPACE_KEY to it }
            ?: NEWS_READING_WORKSPACE_KEY
    }
    override val entries = buildList {
        if (layout.list != null) add(feedEntry)
        if (repliesEntry == null || layout.comments != null) articleEntry?.let(::add)
        repliesEntry?.let(::add)
    }
    private val presentation = AppScenePresentation(
        showsApplicationNavigation = layout.showsNavigation,
        visibleEntryKeys = entries.mapNotNull { it.metadata[APP_ROUTE_KEY] as? NavKey }.toSet(),
    )
    // 公共展示状态与新闻专属布局模式分别声明，装饰场景后仍通过 metadata 读取。
    override val metadata = mapOf(
        APP_PRESENTATION_KEY to presentation,
        NEWS_LAYOUT_MODE_KEY to layout.mode,
    )
    override val content: @Composable () -> Unit = {
        val articleBounds = if (repliesEntry != null) layout.detail else layout.article
        val newsPresentation = NewsPresentation(
            layoutMode = layout.mode,
            selectedArticleId = selected?.articleId,
            listVisible = layout.list != null,
            repliesVisible = repliesEntry != null,
            detailWidth = layout.detail?.width ?: 0f,
            commentsStart = (layout.comments?.left ?: 0f) - (articleBounds?.left ?: 0f),
        )
        CompositionLocalProvider(LocalNewsPresentation provides newsPresentation) {
            val panes = buildList {
                layout.list?.let { add(feedEntry to it) }
                if (articleEntry != null && articleBounds != null && (repliesEntry == null || layout.comments != null)) {
                    add(articleEntry to articleBounds)
                }
                if (repliesEntry != null) (layout.comments ?: layout.detail)?.let { add(repliesEntry to it) }
            }
            AdaptivePaneLayout(panes = panes)
        }
    }
}
