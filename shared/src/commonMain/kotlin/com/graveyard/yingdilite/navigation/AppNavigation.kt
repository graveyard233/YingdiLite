package com.graveyard.yingdilite.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.runtime.collectAsState
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
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.designsystem.media.ImageGalleryPage
import com.graveyard.core.designsystem.media.imageGalleryDialogProperties
import com.graveyard.core.designsystem.navigation.Navigator
import com.graveyard.core.designsystem.navigation.rememberNavigationState
import com.graveyard.core.designsystem.navigation.toEntries
import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.data.repository.AuthRepository
import com.graveyard.feature.cards.navigation.CardsChildRoute
import com.graveyard.feature.cards.navigation.CardsRoute
import com.graveyard.feature.cards.ui.CardsChildScreen
import com.graveyard.feature.cards.ui.CardsScreen
import com.graveyard.feature.community.navigation.CommunityChildRoute
import com.graveyard.feature.community.navigation.CommunityRoute
import com.graveyard.feature.community.ui.CommunityChildScreen
import com.graveyard.feature.community.ui.CommunityScreen
import com.graveyard.feature.mine.navigation.MineLoginRoute
import com.graveyard.feature.mine.navigation.MineAgreementRoute
import com.graveyard.feature.mine.navigation.MineRoute
import com.graveyard.feature.mine.ui.agreement.screen.MineAgreementScreen
import com.graveyard.feature.mine.ui.login.screen.MineLoginRouteScreen
import com.graveyard.feature.mine.ui.effect.MineLoginFeedback
import com.graveyard.feature.mine.ui.mine.screen.MineRouteScreen
import com.graveyard.feature.news.navigation.ArticleRoute
import com.graveyard.feature.news.navigation.ArticleRepliesRoute
import com.graveyard.feature.news.navigation.NewsRoute
import com.graveyard.feature.news.ui.articleDetail.screen.ArticleRouteScreen
import com.graveyard.feature.news.ui.articleDetail.screen.ArticleRepliesRouteScreen
import com.graveyard.feature.news.ui.newsList.screen.NewsRouteScreen
import com.graveyard.yingdilite.navigation.mine.closeMineAgreement
import com.graveyard.yingdilite.navigation.mine.createMineSceneStrategy
import com.graveyard.yingdilite.navigation.mine.isMineAgreementPaneChange
import com.graveyard.yingdilite.navigation.mine.openMineAgreement
import com.graveyard.yingdilite.navigation.mine.openMineLogin
import com.graveyard.yingdilite.navigation.mine.returnToMineProfile
import com.graveyard.yingdilite.navigation.mine.finishMineLogin
import org.koin.compose.koinInject
import com.graveyard.yingdilite.navigation.news.NewsSceneStrategy
import com.graveyard.yingdilite.navigation.news.calculateNewsLayout
import com.graveyard.yingdilite.navigation.news.calculateNewsSingleLayout
import com.graveyard.yingdilite.navigation.news.openImages
import com.graveyard.yingdilite.navigation.news.openLinkedArticle
import com.graveyard.yingdilite.navigation.news.openReplies
import com.graveyard.yingdilite.navigation.news.canInteractWithArticle
import com.graveyard.yingdilite.navigation.news.isRepliesPaneChange
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

// 按路由类型决定是否显示应用导航，带参路由无需构造实例进行相等匹配。
private val hiddenNavigationRouteTypes = listOf(
    CardsChildRoute::class,
    MineLoginRoute::class,
    MineAgreementRoute::class,
    ArticleRoute::class,
    ArticleRepliesRoute::class,
    ImageGalleryRoute::class,
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
                    subclass(ArticleRepliesRoute::class, ArticleRepliesRoute.serializer())
                    subclass(ImageGalleryRoute::class, ImageGalleryRoute.serializer())
                    subclass(CommunityRoute::class, CommunityRoute.serializer())
                    subclass(CommunityChildRoute::class, CommunityChildRoute.serializer())
                    subclass(CardsRoute::class, CardsRoute.serializer())
                    subclass(CardsChildRoute::class, CardsChildRoute.serializer())
                    subclass(MineRoute::class, MineRoute.serializer())
                    subclass(MineLoginRoute::class, MineLoginRoute.serializer())
                    subclass(MineAgreementRoute::class, MineAgreementRoute.serializer())
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
    val authRepository = koinInject<AuthRepository>()
    val authState by authRepository.state.collectAsState()
    val mineStack = navigationState.subStacks.getValue(MineRoute).toList()
    LaunchedEffect(authState is AuthState.Authenticated, mineStack) {
        if (authRepository.state.value is AuthState.Authenticated) navigator.finishMineLogin()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val galleryStrategy = remember { DialogSceneStrategy<NavKey>() }
    val gallery = navigationState.currentKey as? ImageGalleryRoute

    fun handleBack() {
        // A single dispatch can request several pops before the Dialog owns back handling.
        if (gallery != null && navigationState.currentKey != gallery) return
        if (navigationState.canGoBack) {
            navigator.goBack()
        }
    }

    var targetPresentation by remember { mutableStateOf(
        AppScenePresentation(
            showsApplicationNavigation = true,
            visibleEntryKeys = setOf(NewsRoute),
        )
    ) }
    val currentTargetPresentation by rememberUpdatedState(targetPresentation)
    var navigationSize by remember(layoutType) { mutableStateOf(IntSize.Zero) }
    val entries = navigationState.toEntries { key ->
        val galleryMetadata = if (key is ImageGalleryRoute) {
            DialogSceneStrategy.dialog(imageGalleryDialogProperties())
        } else emptyMap()
        NavEntry(
            key = key,
            contentKey = when (key) {
                is ImageGalleryRoute -> "image-gallery:${key.instanceId}"
                is ArticleRoute -> "article:${key.instanceId.ifBlank { key.articleId }}"
                is ArticleRepliesRoute -> "article-replies:${key.instanceId}"
                else -> key.toString()
            },
            metadata = galleryMetadata + mapOf(
                APP_NAVIGATION_SCENE_KEY to key.hasAppNavigation(),
                APP_ROUTE_KEY to key,
                APP_TOP_LEVEL_KEY to navigationState.subStacks.entries.first { key in it.value }.key,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (key == NewsRoute || key == MineRoute || key is ArticleRoute || key is ArticleRepliesRoute || key is ImageGalleryRoute) {
                        Modifier
                    } else Modifier.safeDrawingPadding()),
            ) {
                // 离场场景仍可能可见，但不能继续触发页面操作；弹层下的条目也可能暂时不可交互。
                // 按场景和条目集合共同判断，保留宽屏下多个 pane 同时交互的能力。
                val interactive = LocalSceneInteractive.current && key in currentTargetPresentation.interactiveEntryKeys
                AppNavEntry(
                    key = key,
                    navigator = navigator,
                    authRepository = authRepository,
                    interactive = interactive,
                    onShowMessage = { message ->
                        if (key in currentTargetPresentation.interactiveEntryKeys) {
                            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                        }
                    },
                )
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
            val mineStrategy = createMineSceneStrategy(
                width = maxWidth.value,
                height = maxHeight.value,
                navigationWidth = reservedRailWidth,
                showsNavigation = isRail,
                direction = direction,
                verticalHinges = vertical,
                horizontalHinges = horizontal,
            )
            val underlyingEntries = if (gallery != null) entries.dropLast(1) else entries
            val targetScene = mineStrategy.createScene(underlyingEntries)
                ?: newsStrategy.createScene(underlyingEntries)
            val basePresentation = targetScene?.let(::scenePresentation) ?: AppScenePresentation(
                showsApplicationNavigation = navigationState.currentKey.hasAppNavigation(),
                visibleEntryKeys = setOf(navigationState.currentKey),
            )
            // 图片弹层保留下方页面的显示，只把交互权限交给弹层。
            val resolvedPresentation = if (gallery == null) basePresentation else basePresentation.copy(
                visibleEntryKeys = basePresentation.visibleEntryKeys + gallery,
                interactiveEntryKeys = setOf(gallery),
            )
            androidx.compose.runtime.SideEffect { targetPresentation = resolvedPresentation }
            LaunchedEffect(resolvedPresentation.interactiveEntryKeys) {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            MineLoginFeedback(
                active = navigationState.currentTopLevelKey == MineRoute &&
                    resolvedPresentation.interactiveEntryKeys.any {
                        it == MineRoute || it == MineLoginRoute || it is MineAgreementRoute
                    },
                onShowMessage = { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short) },
            )
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
                            onNavigate = { destination ->
                                if (navigationState.currentKey !is ImageGalleryRoute) navigator.navigate(destination)
                            },
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
                    sceneStrategies = listOf(galleryStrategy, mineStrategy, newsStrategy),
                    sharedTransitionScope = this,
                    transitionSpec = {
                        if (isRepliesPaneChange(initialState, targetState) || isMineAgreementPaneChange(initialState, targetState)) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(NAVIGATION_TRANSITION_DURATION_MILLIS),
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(NAVIGATION_TRANSITION_DURATION_MILLIS),
                        )
                    },
                    popTransitionSpec = {
                        if (isRepliesPaneChange(initialState, targetState) || isMineAgreementPaneChange(initialState, targetState)) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(NAVIGATION_TRANSITION_DURATION_MILLIS),
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(NAVIGATION_TRANSITION_DURATION_MILLIS),
                        )
                    },
                    predictivePopTransitionSpec = {
                        if (isRepliesPaneChange(initialState, targetState) || isMineAgreementPaneChange(initialState, targetState)) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else slideInHorizontally(
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

/**
 * 统一用应用层传入的交互权限保护页面导航回调；页面内滚动、输入等交互由页面自身管理。
 * Navigator 中的栈状态校验仍需保留，用于确认动作执行时的导航上下文是否合法。
 */
@Composable
private fun AppNavEntry(
    key: NavKey,
    navigator: Navigator,
    authRepository: AuthRepository,
    interactive: Boolean,
    onShowMessage: suspend (String) -> Unit,
) {
    when (key) {
        // 新闻内部也使用交互权限控制手势等行为，仅在新闻条目内适配到它的展示状态。
        // copy 保留 NewsScene 提供的布局信息，其他功能不再依赖新闻的 CompositionLocal。
        NewsRoute -> CompositionLocalProvider(
            LocalNewsPresentation provides LocalNewsPresentation.current.copy(interactive = interactive),
        ) {
            NewsRouteScreen(
                onShowMessage = onShowMessage,
                onArticleClick = { articleId, articleTitle, replyNum, likeNum ->
                    if (interactive) navigator.selectArticle(articleId, articleTitle, replyNum, likeNum)
                },
            )
        }
        is ArticleRoute -> CompositionLocalProvider(
            LocalNewsPresentation provides LocalNewsPresentation.current.copy(interactive = interactive),
        ) {
            ArticleRouteScreen(
                articleId = key.articleId,
                articleTitle = key.articleTitle,
                replyNum = key.replyNum,
                likeNum = key.likeNum,
                onBack = { if (interactive && navigator.canInteractWithArticle(key)) navigator.goBack() },
                onImageClick = { urls, index ->
                    if (interactive) navigator.openImages(urls, index, source = key)
                },
                onViewReplies = { commentId -> if (interactive) navigator.openReplies(key, commentId) },
                onCardOpen = { id, title ->
                    // Cards inside an article push another article on top of the reading scene, so
                    // back returns to the article that linked here.
                    if (interactive) navigator.openLinkedArticle(id, title, source = key)
                },
            )
        }
        is ArticleRepliesRoute -> CompositionLocalProvider(
            LocalNewsPresentation provides LocalNewsPresentation.current.copy(interactive = interactive),
        ) {
            ArticleRepliesRouteScreen(
                commentId = key.commentId,
                onBack = { if (interactive && navigator.state.currentKey == key) navigator.goBack() },
                onImageClick = { urls, index ->
                    if (interactive) navigator.openImages(urls, index, source = key)
                },
            )
        }
        is ImageGalleryRoute -> ImageGalleryPage(
            urls = key.urls,
            initialIndex = key.initialIndex,
            onClose = {
                if (interactive && navigator.state.currentKey == key) navigator.goBack()
            },
        )
        CommunityRoute -> CommunityScreen {
            if (interactive) navigator.navigate(CommunityChildRoute)
        }
        // 单页返回还需核对当前路由，避免连续点击在重组前弹出其他页面；卡片子页同理。
        CommunityChildRoute -> CommunityChildScreen(
            onBack = { if (interactive && navigator.state.currentKey == key) navigator.goBack() },
        )
        CardsRoute -> CardsScreen {
            if (interactive) navigator.navigate(CardsChildRoute)
        }
        CardsChildRoute -> CardsChildScreen(
            onBack = { if (interactive && navigator.state.currentKey == key) navigator.goBack() },
        )
        MineRoute -> MineRouteScreen(
            onOpenLogin = { if (interactive) navigator.openMineLogin(authRepository.state.value) },
            onShowMessage = onShowMessage,
        )
        MineLoginRoute -> MineLoginRouteScreen(
            interactive = interactive,
            onBack = { if (interactive) navigator.returnToMineProfile() },
            onOpenAgreement = { if (interactive) navigator.openMineAgreement(it) },
        )
        is MineAgreementRoute -> MineAgreementScreen(
            agreement = key.agreement,
            onBack = { if (interactive) navigator.closeMineAgreement(key) },
        )
        else -> error("Unknown navigation key: $key")
    }
}
