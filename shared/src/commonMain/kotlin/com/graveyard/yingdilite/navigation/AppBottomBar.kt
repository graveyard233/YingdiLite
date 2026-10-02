package com.graveyard.yingdilite.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
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
 * 底部导航栏，取色对齐 `code_light.html` 和 `code_dark.html` 中 `nav` 的颜色定义，
 * 而非 Material 3 的默认角色。明暗模式的导航栏使用独立 token，避免全局主题角色
 * 在两种设计稿之间的语义差异影响底部栏。
 *
 * 只覆盖颜色，不复制设计稿的非颜色差异（顶部描边、上投影、胶囊宽度、标签字号）。
 */
@Composable
fun AppBottomBar(
    darkTheme: Boolean,
    currentKey: NavKey,
    destinations: List<TopLevelDestination>,
    onNavigate: (NavKey) -> Unit,
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

    NavigationBar(
        containerColor = containerColor,
        // 关闭色调叠加，确保导航栏使用对应模式 token 的原色。
        tonalElevation = 0.dp,
    ) {
        destinations.forEach { destination ->
            val selected = destination.key == currentKey
            NavigationBarItem(
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
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedColor,
                    selectedTextColor = selectedColor,
                    indicatorColor = indicatorColor,
                    unselectedIconColor = colorScheme.onSurfaceVariant,
                    unselectedTextColor = colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
