package com.graveyard.yingdilite.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.graveyard.core.designsystem.icons.AppIcon
import com.graveyard.core.designsystem.theme.navigationBarContainerDark
import com.graveyard.core.designsystem.theme.navigationBarContainerLight
import com.graveyard.core.designsystem.theme.navigationBarIndicatorDark
import com.graveyard.core.designsystem.theme.navigationBarIndicatorLight
import com.graveyard.core.designsystem.theme.navigationBarSelectedDark
import com.graveyard.core.designsystem.theme.navigationBarSelectedLight

data class TopLevelDestination(
    val key: NavKey,
    val label: String,
    /** 未选中时显示的线性图标。 */
    val icon: AppIcon,
    /** 选中时显示的填充图标；只有单一版本的图标可省略，默认与 [icon] 相同。 */
    val selectedIcon: AppIcon = icon,
)

/**
 * 在场景装饰器中使用的导航区域：手机显示底部栏，宽屏显示侧栏。
 * 取色对齐 `code_light.html` 和 `code_dark.html` 中 `nav` 的颜色定义。
 *
 * 只覆盖颜色，不复制设计稿的非颜色差异（顶部描边、上投影、胶囊宽度、标签字号）。
 */
@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
@Composable
fun AppNavigationSuite(
    darkTheme: Boolean,
    currentKey: NavKey,
    destinations: List<TopLevelDestination>,
    onNavigate: (NavKey) -> Unit,
    layoutType: NavigationSuiteType,
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (darkTheme) {
        navigationBarContainerDark
    } else {
        navigationBarContainerLight
    }
    val selectedColor = if (darkTheme) {
        navigationBarSelectedDark
    } else {
        navigationBarSelectedLight
    }
    val indicatorColor = if (darkTheme) {
        navigationBarIndicatorDark
    } else {
        navigationBarIndicatorLight
    }

    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = selectedColor,
            selectedTextColor = selectedColor,
            indicatorColor = indicatorColor,
            unselectedIconColor = colorScheme.onSurfaceVariant,
            unselectedTextColor = colorScheme.onSurfaceVariant,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = selectedColor,
            selectedTextColor = selectedColor,
            indicatorColor = indicatorColor,
            unselectedIconColor = colorScheme.onSurfaceVariant,
            unselectedTextColor = colorScheme.onSurfaceVariant,
        ),
    )

    NavigationSuite(
        layoutType = layoutType,
        colors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = containerColor,
            navigationRailContainerColor = containerColor,
        ),
        content = {
            destinations.forEach { destination ->
                val selected = destination.key == currentKey
                item(
                    selected = selected,
                    onClick = { onNavigate(destination.key) },
                    icon = {
                        Icon(
                            imageVector = (if (selected) {
                                destination.selectedIcon
                            } else {
                                destination.icon
                            }).imageVector,
                            contentDescription = destination.label,
                        )
                    },
                    label = { Text(destination.label) },
                    // 只有选中项显示文本标签，未选中项仅保留图标。
                    alwaysShowLabel = false,
                    colors = itemColors,
                )
            }
        },
    )
}
