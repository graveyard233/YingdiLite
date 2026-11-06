package com.graveyard.yingdilite.navigation.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.graveyard.yingdilite.navigation.APP_PRESENTATION_KEY
import com.graveyard.yingdilite.navigation.APP_ROUTE_KEY
import com.graveyard.yingdilite.navigation.APP_TOP_LEVEL_KEY
import com.graveyard.yingdilite.navigation.AdaptivePaneLayout
import com.graveyard.yingdilite.navigation.AppScenePresentation
import com.graveyard.yingdilite.navigation.PaneBounds
import com.graveyard.yingdilite.navigation.scenePresentation

internal fun createAuthSceneStrategy(
    width: Float,
    height: Float,
    navigationWidth: Float,
    showsNavigation: Boolean,
    direction: LayoutDirection,
    verticalHinges: List<PaneBounds>,
    horizontalHinges: List<PaneBounds>,
): AuthSceneStrategy = AuthSceneStrategy(
    calculateLoginLayout(
        width = width,
        height = height,
        navigationWidth = navigationWidth,
        showsNavigation = showsNavigation,
        direction = direction,
        verticalHinges = verticalHinges,
        horizontalHinges = horizontalHinges,
    ),
)

internal data class AuthSceneStrategy(val layout: LoginLayout) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? =
        createScene(entries)

    fun createScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        val last = entries.lastOrNull() ?: return null
        val owner = last.metadata[APP_TOP_LEVEL_KEY]
        val route = last.metadata[APP_ROUTE_KEY]
        if (route !is LoginRoute && route !is LoginAgreementRoute) return null
        val login = entries.getOrNull(entries.lastIndex - 1)?.takeIf {
            val key = it.metadata[APP_ROUTE_KEY] as? LoginRoute
            key != null && (route as? LoginAgreementRoute)?.loginInstanceId == key.instanceId &&
                it.metadata[APP_TOP_LEVEL_KEY] == owner
        }
        val panes = if (route is LoginAgreementRoute && login != null &&
            layout.login != null && layout.agreement != null
        ) {
            listOf(login to layout.login, last to layout.agreement)
        } else listOf(last to layout.single)
        return AuthScene(layout.showsNavigation, panes, entries.dropLast(1))
    }
}

private data class AuthScene(
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

internal fun isLoginAgreementPaneChange(initial: Scene<NavKey>, target: Scene<NavKey>): Boolean {
    val from = scenePresentation(initial).visibleEntryKeys
    val to = scenePresentation(target).visibleEntryKeys
    return from.filterIsInstance<LoginRoute>().any { it in to } &&
        (from.any { it is LoginAgreementRoute } || to.any { it is LoginAgreementRoute })
}
