package com.graveyard.yingdilite.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onExit: (() -> Unit)? = null,
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
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var rootBackPromptVisible by remember { mutableStateOf(false) }

    LaunchedEffect(rootBackPromptVisible) {
        if (rootBackPromptVisible) {
            delay(ROOT_BACK_CONFIRMATION_WINDOW_MILLIS)
            rootBackPromptVisible = false
        }
    }

    // 路由变化后开始新的返回流程，不能继承之前根页面的确认状态。
    LaunchedEffect(navigationState.currentKey) {
        rootBackPromptVisible = false
    }

    fun handleBack() {
        if (navigationState.canGoBack) {
            rootBackPromptVisible = false
            navigator.goBack()
            return
        }

        // iOS 没有 Android 式的系统返回退出行为，因此根节点不显示退出提示。
        if (onExit == null) return

        if (rootBackPromptVisible) {
            rootBackPromptVisible = false
            onExit.invoke()
            return
        }

        rootBackPromptVisible = true
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = "再按一次退出应用",
                duration = SnackbarDuration.Short,
            )
        }
    }

    val entries = navigationState.toEntries { key ->
        NavEntry(key, metadata = mapOf(APP_NAVIGATION_SCENE_KEY to key.hasAppNavigation())) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (key == NewsRoute) Modifier else Modifier.safeDrawingPadding()),
            ) {
                AppNavEntry(
                    key = key,
                    navigator = navigator,
                    onShowMessage = { message ->
                        if (navigationState.currentKey == key) {
                            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                        }
                    },
                )
            }
        }
    }

    val appBackHandlerState = rememberNavigationEventState(
        currentInfo = AppNavigationEventInfo,
        backInfo = listOf(AppNavigationEventInfo),
        forwardInfo = emptyList(),
    )

    Surface(color = MaterialTheme.colorScheme.background) {
        SharedTransitionLayout(Modifier.fillMaxSize()) {
            val navigationSceneDecorator = rememberAppNavigationSceneDecoratorStrategy(
                layoutType = layoutType,
                targetHasNavigation = navigationState.currentKey.hasAppNavigation(),
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
        if (onExit != null) {
            NavigationBackHandler(
                state = appBackHandlerState,
                isBackEnabled = onExit != null && !navigationState.canGoBack,
                onBackCancelled = {},
                onBackCompleted = ::handleBack,
            )
        }
    }
}

private fun NavKey.hasAppNavigation(): Boolean = this::class !in hiddenNavigationRouteTypes

private const val ROOT_BACK_CONFIRMATION_WINDOW_MILLIS = 2_000L
private const val NAVIGATION_TRANSITION_DURATION_MILLIS = 300

private object AppNavigationEventInfo : NavigationEventInfo()

@Composable
private fun AppNavEntry(
    key: NavKey,
    navigator: Navigator,
    onShowMessage: suspend (String) -> Unit,
) {
    when (key) {
        NewsRoute -> NewsRouteScreen(
            onShowMessage = onShowMessage,
            onArticleClick = { articleId,articleTitle -> navigator.navigate(ArticleRoute(articleId,articleTitle)) },
        )
        is ArticleRoute -> ArticleRouteScreen(
            articleId = key.articleId,
            articleTitle = key.articleTitle,
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
