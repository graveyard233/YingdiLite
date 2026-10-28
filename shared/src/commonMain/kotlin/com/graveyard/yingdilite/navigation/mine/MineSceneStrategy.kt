package com.graveyard.yingdilite.navigation.mine

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.graveyard.feature.mine.navigation.MineAgreementRoute
import com.graveyard.feature.mine.navigation.MineLoginRoute
import com.graveyard.feature.mine.navigation.MineRoute
import com.graveyard.yingdilite.navigation.APP_PRESENTATION_KEY
import com.graveyard.yingdilite.navigation.APP_ROUTE_KEY
import com.graveyard.yingdilite.navigation.APP_TOP_LEVEL_KEY
import com.graveyard.yingdilite.navigation.AdaptivePaneLayout
import com.graveyard.yingdilite.navigation.AppScenePresentation
import com.graveyard.yingdilite.navigation.PaneBounds
import com.graveyard.yingdilite.navigation.scenePresentation

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
    // 路由决定场景身份；窗口尺寸变化只调整 pane 边界，保留原有条目标识。
    override val key: Any = panes.last().first.contentKey
    override val entries = panes.map { it.first }
    override val metadata = mapOf(
        APP_PRESENTATION_KEY to AppScenePresentation(
            showsApplicationNavigation = showsNavigation,
            visibleEntryKeys = entries.mapNotNull { it.metadata[APP_ROUTE_KEY] as? NavKey }.toSet(),
        ),
    )
    override val content: @Composable () -> Unit = {
        AdaptivePaneLayout(panes = panes)
    }
}

internal fun isMineAgreementPaneChange(initial: Scene<NavKey>, target: Scene<NavKey>): Boolean {
    val from = scenePresentation(initial).visibleEntryKeys
    val to = scenePresentation(target).visibleEntryKeys
    return MineLoginRoute in from && MineLoginRoute in to &&
        (from.any { it is MineAgreementRoute } || to.any { it is MineAgreementRoute })
}
