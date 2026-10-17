package com.graveyard.yingdilite.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import com.graveyard.feature.news.navigation.LocalNewsPresentation
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.designsystem.navigation.Navigator
import com.graveyard.core.designsystem.navigation.rememberNavigationState
import com.graveyard.core.designsystem.navigation.toEntries
import com.graveyard.feature.cards.navigation.CardsChildRoute
import com.graveyard.feature.cards.navigation.CardsRoute
import com.graveyard.feature.cards.ui.CardsChildScreen
import com.graveyard.feature.cards.ui.CardsScreen
import com.graveyard.feature.community.navigation.CommunityChildRoute
import com.graveyard.feature.community.navigation.CommunityRoute
import com.graveyard.feature.community.ui.CommunityChildScreen
import com.graveyard.feature.community.ui.CommunityScreen
import com.graveyard.feature.mine.navigation.MineChildRoute
import com.graveyard.feature.mine.navigation.MineRoute
import com.graveyard.feature.mine.ui.MineChildScreen
import com.graveyard.feature.mine.ui.MineScreen
import com.graveyard.feature.news.navigation.ArticleRoute
import com.graveyard.feature.news.navigation.NewsChildRoute
import com.graveyard.feature.news.navigation.NewsRoute
import com.graveyard.feature.news.ui.articleDetail.screen.ArticleRouteScreen
import com.graveyard.feature.news.ui.newsList.screen.NewsChildScreen
import com.graveyard.feature.news.ui.newsList.screen.NewsRouteScreen
import com.graveyard.yingdilite.navigation.news.APP_ROUTE_KEY
import com.graveyard.yingdilite.navigation.news.APP_TOP_LEVEL_KEY
import com.graveyard.yingdilite.navigation.news.AppScenePresentation
import com.graveyard.yingdilite.navigation.news.NewsSceneStrategy
import com.graveyard.yingdilite.navigation.news.PaneBounds
import com.graveyard.yingdilite.navigation.news.calculateNewsLayout
import com.graveyard.yingdilite.navigation.news.calculateNewsSingleLayout
import com.graveyard.yingdilite.navigation.news.scenePresentation
import com.graveyard.yingdilite.navigation.news.selectArticle
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

private val topLevelDestinations = listOf(
    TopLevelDestination(NewsRoute, "新闻", icon = AppIcon.News, selectedIcon = AppIcon.NewsFill),
    TopLevelDestination(CommunityRoute, "社区", icon = AppIcon.Community, selectedIcon = AppIcon.CommunityFill),
    TopLevelDestination(CardsRoute, "卡片", icon = AppIcon.Cards, selectedIcon = AppIcon.CardsFill),
    TopLevelDestination(MineRoute, "我的", icon = AppIcon.AccountCircle, selectedIcon = AppIcon.AccountCircleFill),
)

private val topLevelKeys = topLevelDestinations.map(TopLevelDestination::key)

// 带参路由无法放进 setOf<NavKey> 做相等匹配，因此按类型决定是否装饰场景。
private val hiddenNavigationRouteTypes = setOf(
    CardsChildRoute::class,
    MineChildRoute::class,
    ArticleRoute::class,
)

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
@Suppress("DEPRECATION") // 与 NavigationSuite 的默认自适应布局计算保持一致。
@Composable
fun AppNavigation(
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    val configuration = remember {
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(NewsRoute::class, NewsRoute.serializer())
                    subclass(ArticleRoute::class, ArticleRoute.serializer())
                    subclass(NewsChildRoute::class, NewsChildRoute.serializer())
                    subclass(CommunityRoute::class, CommunityRoute.serializer())
                    subclass(CommunityChildRoute::class, CommunityChildRoute.serializer())
                    subclass(CardsRoute::class, CardsRoute.serializer())
                    subclass(CardsChildRoute::class, CardsChildRoute.serializer())
                    subclass(MineRoute::class, MineRoute.serializer())
                    subclass(MineChildRoute::class, MineChildRoute.serializer())
                }
            }
        }
    }

    val navigationState = rememberNavigationState(
        configuration = configuration,
        startKey = NewsRoute,
        topLevelKeys = topLevelKeys,
    )
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun handleBack() {
        if (navigationState.canGoBack) {
            navigator.goBack()
        }
    }

    var targetPresentation by remember { mutableStateOf(
        AppScenePresentation(
            null,
            true,
            setOf(NewsRoute)
        )
    ) }
    val currentTargetPresentation by rememberUpdatedState(targetPresentation)
    var navigationSize by remember(layoutType) { mutableStateOf(IntSize.Zero) }
    val entries = navigationState.toEntries { key ->
        NavEntry(key, metadata = mapOf(
            APP_NAVIGATION_SCENE_KEY to key.hasAppNavigation(),
            APP_ROUTE_KEY to key,
            APP_TOP_LEVEL_KEY to navigationState.subStacks.entries.first { key in it.value }.key,
        )) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (key == NewsRoute || key is ArticleRoute) Modifier else Modifier.safeDrawingPadding()),
            ) {
                val interactive = LocalSceneInteractive.current && key in currentTargetPresentation.interactiveEntryKeys
                CompositionLocalProvider(LocalNewsPresentation provides LocalNewsPresentation.current.copy(interactive = interactive)) {
                    AppNavEntry(
                        key = key,
                        navigator = navigator,
                        onShowMessage = { message ->
                            if (key in currentTargetPresentation.interactiveEntryKeys) {
                                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                            }
                        },
                    )
                }
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            val density = LocalDensity.current
            val direction = LocalLayoutDirection.current
            val safeLeft = WindowInsets.safeDrawing.getLeft(density, direction) / density.density
            val isRail = layoutType == NavigationSuiteType.NavigationRail
            val railWidth = if (isRail) navigationSize.width / density.density else 0f
            val reservedRailWidth = if (isRail && railWidth == 0f) 80f else railWidth
            val barHeight = if (layoutType == NavigationSuiteType.NavigationBar) navigationSize.height / density.density else 0f
            val hinges = adaptiveInfo.windowPosture.hingeList.filter { it.isSeparating || it.isOccluding }
            val vertical = hinges.filter { it.isVertical }.map {
                PaneBounds(
                    it.bounds.left / density.density - safeLeft, it.bounds.top / density.density,
                    it.bounds.width / density.density, it.bounds.height / density.density
                )
            }
            val horizontal = hinges.filter { !it.isVertical }.map {
                PaneBounds(
                    it.bounds.left / density.density - safeLeft, it.bounds.top / density.density,
                    it.bounds.width / density.density, it.bounds.height / density.density
                )
            }
            // Reserve the potential rail before choosing a mode to avoid hide/show oscillation.
            fun calculate(reading: Boolean, reserveNavigation: Boolean) = calculateNewsLayout(
                width = maxWidth.value,
                height = maxHeight.value - if (reserveNavigation) barHeight else 0f,
                reading = reading,
                navigationWidth = if (reserveNavigation) reservedRailWidth else 0f,
                verticalHinges = if (direction == LayoutDirection.Rtl && reserveNavigation) {
                    vertical.map { it.copy(left = it.left + reservedRailWidth) }
                } else vertical,
                horizontalHinges = horizontal,
            )
            val feedLayout = calculate(reading = false, reserveNavigation = true)
            val candidate = calculate(reading = true, reserveNavigation = true)
            val readingLayout = if (candidate.showsNavigation) candidate else {
                // Reclaim rail space for the article; do not promote the single-pane decision.
                calculateNewsSingleLayout(maxWidth.value, maxHeight.value, vertical, horizontal)
            }
            val newsStrategy = NewsSceneStrategy(feedLayout, readingLayout)
            val targetNewsScene = newsStrategy.createScene(entries)
            val resolvedPresentation = targetNewsScene?.let(::scenePresentation) ?: AppScenePresentation(
                null,
                navigationState.currentKey.hasAppNavigation(),
                setOf(navigationState.currentKey)
            )
            androidx.compose.runtime.SideEffect { targetPresentation = resolvedPresentation }
            LaunchedEffect(resolvedPresentation.interactiveEntryKeys) {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            SharedTransitionLayout(Modifier.fillMaxSize()) {
                val navigationSceneDecorator = rememberAppNavigationSceneDecoratorStrategy(
                    layoutType = layoutType,
                    targetHasNavigation = resolvedPresentation.showsApplicationNavigation,
                    onNavigationSizeChanged = { navigationSize = it },
                    navigationSuite = {
                        AppNavigationSuite(
                            darkTheme = darkTheme,
                            currentKey = navigationState.currentTopLevelKey,
                            destinations = topLevelDestinations,
                            onNavigate = navigator::navigate,
                            layoutType = layoutType,
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    sharedTransitionScope = this,
                )
                NavDisplay(
                    entries = entries,
                    onBack = ::handleBack,
                    modifier = Modifier.fillMaxSize(),
                    sceneDecoratorStrategies = listOf(navigationSceneDecorator),
                    sceneStrategies = listOf(newsStrategy),
                    sharedTransitionScope = this,
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(NAVIGATION_TRANSITION_DURATION_MILLIS),
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(NAVIGATION_TRANSITION_DURATION_MILLIS),
                        )
                    },
                    popTransitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(NAVIGATION_TRANSITION_DURATION_MILLIS),
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(NAVIGATION_TRANSITION_DURATION_MILLIS),
                        )
                    },
                    predictivePopTransitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(NAVIGATION_TRANSITION_DURATION_MILLIS),
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(NAVIGATION_TRANSITION_DURATION_MILLIS),
                        )
                    },
                )
            }
        }
    }
}

private fun NavKey.hasAppNavigation(): Boolean = this::class !in hiddenNavigationRouteTypes

private const val NAVIGATION_TRANSITION_DURATION_MILLIS = 300

@Composable
private fun AppNavEntry(
    key: NavKey,
    navigator: Navigator,
    onShowMessage: suspend (String) -> Unit,
) {
    when (key) {
        NewsRoute -> NewsRouteScreen(
            onShowMessage = onShowMessage,
            onArticleClick = { articleId, articleTitle, newsAuthor ->
                navigator.selectArticle(articleId, articleTitle, newsAuthor)
            },
        )
        is ArticleRoute -> ArticleRouteScreen(
            articleId = key.articleId,
            articleTitle = key.articleTitle,
            newsAuthor = key.newsAuthor,
            onBack = navigator::goBack,
        )
        NewsChildRoute -> NewsChildScreen(navigator::goBack)
        CommunityRoute -> CommunityScreen { navigator.navigate(CommunityChildRoute) }
        CommunityChildRoute -> CommunityChildScreen(navigator::goBack)
        CardsRoute -> CardsScreen { navigator.navigate(CardsChildRoute) }
        CardsChildRoute -> CardsChildScreen(navigator::goBack)
        MineRoute -> MineScreen { navigator.navigate(MineChildRoute) }
        MineChildRoute -> MineChildScreen(navigator::goBack)
        else -> error("Unknown navigation key: $key")
    }
}
