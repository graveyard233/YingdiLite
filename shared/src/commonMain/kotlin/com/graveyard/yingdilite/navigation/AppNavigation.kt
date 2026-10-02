package com.graveyard.yingdilite.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.graveyard.feature.news.navigation.NewsChildRoute
import com.graveyard.feature.news.navigation.NewsRoute
import com.graveyard.feature.news.ui.NewsChildScreen
import com.graveyard.feature.news.ui.NewsScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val topLevelDestinations = listOf(
    TopLevelDestination(NewsRoute, "新闻", icon = AppIcon.News, selectedIcon = AppIcon.NewsFill),
    TopLevelDestination(CommunityRoute, "社区", icon = AppIcon.Community, selectedIcon = AppIcon.CommunityFill),
    TopLevelDestination(CardsRoute, "卡片", icon = AppIcon.Cards, selectedIcon = AppIcon.CardsFill),
    TopLevelDestination(MineRoute, "我的", icon = AppIcon.AccountCircle, selectedIcon = AppIcon.AccountCircleFill),
)

private val topLevelKeys = topLevelDestinations.map(TopLevelDestination::key)

private val hiddenBottomBarRoutes = setOf<NavKey>(
    CardsChildRoute,
    MineChildRoute,
)

@Composable
fun AppNavigation(
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    val configuration = remember {
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(NewsRoute::class, NewsRoute.serializer())
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
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val entries = navigationState.toEntries { key ->
        NavEntry(key) {
            AppNavEntry(key = key, navigator = navigator)
        }
    }

    Scaffold(
        bottomBar = {
            if (navigationState.currentKey !in hiddenBottomBarRoutes) {
                AppBottomBar(
                    darkTheme = darkTheme,
                    currentKey = navigationState.currentTopLevelKey,
                    destinations = topLevelDestinations,
                    onNavigate = navigator::navigate,
                )
            }
        },
    ) { paddingValues ->
        NavDisplay(
            entries = entries,
            onBack = navigator::goBack,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun AppNavEntry(
    key: NavKey,
    navigator: Navigator,
) {
    when (key) {
        NewsRoute -> NewsScreen { navigator.navigate(NewsChildRoute) }
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
