package com.graveyard.yingdilite.navigation.news

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.graveyard.feature.news.navigation.ArticleRoute
import com.graveyard.feature.news.navigation.LocalNewsPresentation
import com.graveyard.feature.news.navigation.NewsLayoutMode
import com.graveyard.feature.news.navigation.NewsPresentation
import com.graveyard.feature.news.navigation.NewsRoute
import com.graveyard.yingdilite.navigation.APP_NAVIGATION_SCENE_KEY
import kotlin.math.roundToInt

internal const val APP_ROUTE_KEY = "com.graveyard.yingdilite.route"
internal const val APP_TOP_LEVEL_KEY = "com.graveyard.yingdilite.topLevel"
internal const val APP_PRESENTATION_KEY = "com.graveyard.yingdilite.presentation"

internal data class AppScenePresentation(
    val layoutMode: NewsLayoutMode?,
    val showsApplicationNavigation: Boolean,
    val visibleEntryKeys: Set<NavKey>,
    val interactiveEntryKeys: Set<NavKey> = visibleEntryKeys,
)

internal fun scenePresentation(scene: Scene<NavKey>): AppScenePresentation {
    (scene.metadata[APP_PRESENTATION_KEY] as? AppScenePresentation)?.let { return it }
    val keys = scene.entries.mapNotNull { it.metadata[APP_ROUTE_KEY] as? NavKey }.toSet()
    // Non-news multi-entry scenes must opt in via their own scene presentation.
    return AppScenePresentation(null,
        scene.entries.size == 1 && scene.metadata[APP_NAVIGATION_SCENE_KEY] == true, keys)
}

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
        if (route !is ArticleRoute) return null
        // A contiguous feed/selection segment, never another tab's historic feed.
        val feedIndex = entries.indexOfLast { it.metadata[APP_ROUTE_KEY] == NewsRoute }
        if (feedIndex < 0 || entries.subList(feedIndex, entries.size).any {
                it.metadata[APP_TOP_LEVEL_KEY] != NewsRoute ||
                    (it.metadata[APP_ROUTE_KEY] != NewsRoute && it.metadata[APP_ROUTE_KEY] !is ArticleRoute)
            }) return null
        return NewsScene(readingLayout, entries[feedIndex], last, entries.dropLast(1))
    }
}

internal data class NewsScene(
    val layout: NewsLayout,
    val feedEntry: NavEntry<NavKey>,
    val articleEntry: NavEntry<NavKey>?,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    // A wide reading workspace remains stable when the selected article changes.
    override val key: Any = when (layout.mode) {
        NewsLayoutMode.Feed -> "news-feed"
        else -> "news-reading-workspace"
    }
    override val entries = buildList {
        if (layout.list != null) add(feedEntry)
        articleEntry?.let(::add)
    }
    private val selected = articleEntry?.metadata?.get(APP_ROUTE_KEY) as? ArticleRoute
    private val presentation = AppScenePresentation(
        layout.mode, layout.showsNavigation,
        entries.mapNotNull { it.metadata[APP_ROUTE_KEY] as? NavKey }.toSet(),
    )
    override val metadata = mapOf(APP_PRESENTATION_KEY to presentation)
    override val content: @Composable () -> Unit = {
        val articleBounds = layout.article
        val newsPresentation = NewsPresentation(
            layoutMode = layout.mode,
            selectedArticleId = selected?.articleId,
            listVisible = layout.list != null,
            detailWidth = layout.detail?.width ?: 0f,
            commentsStart = (layout.comments?.left ?: 0f) - (articleBounds?.left ?: 0f),
        )
        CompositionLocalProvider(LocalNewsPresentation provides newsPresentation) {
            val panes = buildList {
                layout.list?.let { add(feedEntry to it) }
                if (articleEntry != null && articleBounds != null) add(articleEntry to articleBounds)
            }
            Layout(modifier = Modifier.fillMaxSize(), content = {
                panes.forEach { (entry, _) -> key(entry.contentKey) { Box { entry.Content() } } }
            }) { measurables, constraints ->
                val bounds = panes.map { it.second }
                val placements = measurables.zip(bounds).map { (measurable, pane) ->
                    val x = (pane.left * density).roundToInt().coerceIn(0, constraints.maxWidth)
                    val y = (pane.top * density).roundToInt().coerceIn(0, constraints.maxHeight)
                    val width = (pane.width * density).roundToInt().coerceIn(0, constraints.maxWidth - x)
                    val height = (pane.height * density).roundToInt().coerceIn(0, constraints.maxHeight - y)
                    Triple(measurable.measure(Constraints.fixed(width, height)), x, y)
                }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placements.forEach { (placeable, x, y) -> placeable.place(x, y) }
                }
            }
        }
    }
}
