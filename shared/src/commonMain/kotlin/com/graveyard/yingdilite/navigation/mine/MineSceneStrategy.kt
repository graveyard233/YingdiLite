package com.graveyard.yingdilite.navigation.mine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.graveyard.feature.mine.navigation.MineAgreementRoute
import com.graveyard.feature.mine.navigation.MineLoginRoute
import com.graveyard.feature.mine.navigation.MineRoute
import com.graveyard.yingdilite.navigation.PaneBounds
import com.graveyard.yingdilite.navigation.news.APP_PRESENTATION_KEY
import com.graveyard.yingdilite.navigation.news.APP_ROUTE_KEY
import com.graveyard.yingdilite.navigation.news.APP_TOP_LEVEL_KEY
import com.graveyard.yingdilite.navigation.news.AppScenePresentation
import com.graveyard.yingdilite.navigation.news.scenePresentation
import kotlin.math.roundToInt

internal fun createMineSceneStrategy(
    width: Float,
    height: Float,
    navigationWidth: Float,
    showsNavigation: Boolean,
    direction: LayoutDirection,
    verticalHinges: List<PaneBounds>,
    horizontalHinges: List<PaneBounds>,
): MineSceneStrategy = MineSceneStrategy(
    calculateMineLayout(
        width = width,
        height = height,
        navigationWidth = navigationWidth,
        showsNavigation = showsNavigation,
        direction = direction,
        verticalHinges = verticalHinges,
        horizontalHinges = horizontalHinges,
    ),
)

internal data class MineSceneStrategy(val layout: MineLayout) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? =
        createScene(entries)

    fun createScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        val last = entries.lastOrNull() ?: return null
        if (last.metadata[APP_TOP_LEVEL_KEY] != MineRoute) return null
        val route = last.metadata[APP_ROUTE_KEY]
        if (route != MineLoginRoute && route !is MineAgreementRoute) return null
        val login = entries.getOrNull(entries.lastIndex - 1)?.takeIf {
            it.metadata[APP_ROUTE_KEY] == MineLoginRoute && it.metadata[APP_TOP_LEVEL_KEY] == MineRoute
        }
        val panes = if (route is MineAgreementRoute && login != null &&
            layout.login != null && layout.agreement != null
        ) {
            listOf(login to layout.login, last to layout.agreement)
        } else listOf(last to layout.single)
        return MineScene(layout.showsNavigation, panes, entries.dropLast(1))
    }
}

private data class MineScene(
    val showsNavigation: Boolean,
    val panes: List<Pair<NavEntry<NavKey>, PaneBounds>>,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    // The active route, not window size, owns the scene and entry identities.
    override val key: Any = panes.last().first.contentKey
    override val entries = panes.map { it.first }
    override val metadata = mapOf(
        APP_PRESENTATION_KEY to AppScenePresentation(
            layoutMode = null,
            showsApplicationNavigation = showsNavigation,
            visibleEntryKeys = entries.mapNotNull { it.metadata[APP_ROUTE_KEY] as? NavKey }.toSet(),
        ),
    )
    override val content: @Composable () -> Unit = {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                panes.forEach { (entry, _) -> key(entry.contentKey) { Box { entry.Content() } } }
            },
        ) { measurables, constraints ->
            val placements = measurables.zip(panes).map { (measurable, pane) ->
                val bounds = pane.second
                val x = (bounds.left * density).roundToInt().coerceIn(0, constraints.maxWidth)
                val y = (bounds.top * density).roundToInt().coerceIn(0, constraints.maxHeight)
                val width = (bounds.width * density).roundToInt().coerceIn(0, constraints.maxWidth - x)
                val height = (bounds.height * density).roundToInt().coerceIn(0, constraints.maxHeight - y)
                Triple(measurable.measure(Constraints.fixed(width, height)), x, y)
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                placements.forEach { (placeable, x, y) -> placeable.place(x, y) }
            }
        }
    }
}

internal fun isMineAgreementPaneChange(initial: Scene<NavKey>, target: Scene<NavKey>): Boolean {
    val from = scenePresentation(initial).visibleEntryKeys
    val to = scenePresentation(target).visibleEntryKeys
    return MineLoginRoute in from && MineLoginRoute in to &&
        (from.any { it is MineAgreementRoute } || to.any { it is MineAgreementRoute })
}
