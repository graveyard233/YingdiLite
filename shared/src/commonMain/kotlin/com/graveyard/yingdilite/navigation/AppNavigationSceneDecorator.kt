package com.graveyard.yingdilite.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope

internal const val APP_NAVIGATION_SCENE_KEY = "com.graveyard.yingdilite.navigation.hasNavigation"

@Composable
internal fun rememberAppNavigationSceneDecoratorStrategy(
    layoutType: NavigationSuiteType,
    targetHasNavigation: Boolean,
    navigationSuite: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
): SceneDecoratorStrategy<NavKey> {
    val currentNavigationSuite by rememberUpdatedState(navigationSuite)
    val currentSnackbarHost by rememberUpdatedState(snackbarHost)
    val currentTargetHasNavigation by rememberUpdatedState(targetHasNavigation)
    val movableNavigationSuite = remember { movableContentOf { currentNavigationSuite() } }
    val movableSnackbarHost = remember { movableContentOf { currentSnackbarHost() } }

    return remember(layoutType, sharedTransitionScope) {
        AppNavigationSceneDecoratorStrategy(
            layoutType = layoutType,
            sharedTransitionScope = sharedTransitionScope,
            navigationSuite = movableNavigationSuite,
            snackbarHost = movableSnackbarHost,
            targetHasNavigation = { currentTargetHasNavigation },
        )
    }
}

private class AppNavigationSceneDecoratorStrategy(
    private val layoutType: NavigationSuiteType,
    private val sharedTransitionScope: SharedTransitionScope,
    private val navigationSuite: @Composable () -> Unit,
    private val snackbarHost: @Composable () -> Unit,
    private val targetHasNavigation: () -> Boolean,
) : SceneDecoratorStrategy<NavKey> {
    override fun SceneDecoratorStrategyScope<NavKey>.decorateScene(
        scene: Scene<NavKey>,
    ): Scene<NavKey> {
        // 判断当前场景自身，而不是变化后的全局 currentKey；离场场景保留原导航栏。
        if (scene.entries.singleOrNull()?.metadata?.get(APP_NAVIGATION_SCENE_KEY) != true) {
            return scene
        }
        return AppNavigationScene(
            scene = scene,
            layoutType = layoutType,
            sharedTransitionScope = sharedTransitionScope,
            navigationSuite = navigationSuite,
            snackbarHost = snackbarHost,
            targetHasNavigation = targetHasNavigation,
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
private data class AppNavigationScene(
    private val scene: Scene<NavKey>,
    private val layoutType: NavigationSuiteType,
    private val sharedTransitionScope: SharedTransitionScope,
    private val navigationSuite: @Composable () -> Unit,
    private val snackbarHost: @Composable () -> Unit,
    private val targetHasNavigation: () -> Boolean,
) : Scene<NavKey> by scene {
    override val key: Any = scene::class to scene.key

    override val content: @Composable () -> Unit = {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        // Tab 间切换由入场场景绘制共享栏；进入全屏页则由离场场景继续绘制到动画结束。
        val drawSharedContent =
            animatedContentScope.transition.targetState == EnterExitState.Visible ||
                !targetHasNavigation()
        val consumedInsets = when (layoutType) {
            NavigationSuiteType.NavigationBar ->
                NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)
            NavigationSuiteType.NavigationRail ->
                NavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
            else -> WindowInsets(0, 0, 0, 0)
        }

        with(sharedTransitionScope) {
            NavigationSuiteScaffoldLayout(
                layoutType = layoutType,
                navigationSuite = {
                    Box(
                        Modifier
                            .cacheNavigationSize(!drawSharedContent)
                            .sharedElement(
                                rememberSharedContentState(
                                    if (layoutType == NavigationSuiteType.NavigationBar) {
                                        "app-navigation-bar"
                                    } else {
                                        "app-navigation-rail"
                                    },
                                ),
                                animatedContentScope,
                            ),
                    ) {
                        if (drawSharedContent) navigationSuite()
                    }
                },
            ) {
                Box(Modifier.fillMaxSize().consumeWindowInsets(consumedInsets)) {
                    scene.content()
                    if (drawSharedContent) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                                ),
                        ) {
                            snackbarHost()
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.cacheNavigationSize(useCachedSize: Boolean): Modifier =
    this.then(NavigationSizeElement(useCachedSize))

private data class NavigationSizeElement(
    val useCachedSize: Boolean,
) : ModifierNodeElement<NavigationSizeNode>() {
    override fun create(): NavigationSizeNode = NavigationSizeNode(useCachedSize)

    override fun update(node: NavigationSizeNode) {
        node.useCachedSize = useCachedSize
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "cacheNavigationSize"
        properties["useCachedSize"] = useCachedSize
    }
}

private class NavigationSizeNode(
    useCachedSize: Boolean,
) : Modifier.Node(), LayoutModifierNode {
    var useCachedSize: Boolean = useCachedSize
        set(value) {
            if (field != value) {
                field = value
                invalidateMeasurement()
            }
        }

    private var cachedSize: IntSize? = null

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val measuredSize = IntSize(placeable.width, placeable.height)
        val size = if (useCachedSize) cachedSize ?: measuredSize else measuredSize
        if (!useCachedSize) cachedSize = measuredSize

        return layout(size.width, size.height) {
            placeable.placeRelative(0, 0)
        }
    }
}
