package com.graveyard.core.designsystem.navigation


import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration

/**
 * 创建一个能够在配置变更和进程被杀死后依然保留的导航状态。
 *
 * @param configuration - 已保存状态（saved state）配置，其 `serializersModule` 注册了所有
 * [NavKey] 子类，因此各个返回栈在所有平台上都能在进程被杀死后恢复。
 */
@Composable
fun rememberNavigationState(
    configuration: SavedStateConfiguration,
    startKey: NavKey,
    topLevelKeys: List<NavKey>,
): NavigationState {
    require(startKey in topLevelKeys) {
        "The start key must be included in the top-level keys"
    }

    val topLevelStack = rememberNavBackStack(configuration, startKey)
    val subStacks = topLevelKeys.associateWith { key -> rememberNavBackStack(configuration, key) }

    return remember(startKey, topLevelKeys) {
        NavigationState(
            startKey = startKey,
            topLevelStack = topLevelStack,
            subStacks = subStacks,
        )
    }
}

/**
 * 导航状态的状态持有者。
 *
 * @param startKey - 起始导航键。用户将通过该键退出应用。
 * @param topLevelStack - 顶层返回栈，其中只保存顶层键。
 * @param subStacks - 每个顶层键对应的返回栈。
 */
class NavigationState(
    val startKey: NavKey,
    val topLevelStack: NavBackStack<NavKey>,
    val subStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    val currentTopLevelKey: NavKey by derivedStateOf { topLevelStack.last() }

    val topLevelKeys get() = subStacks.keys

    val currentSubStack: NavBackStack<NavKey>
        get() = subStacks[currentTopLevelKey]
            ?: error("Sub stack for $currentTopLevelKey does not exist")

    val currentKey: NavKey by derivedStateOf { currentSubStack.last() }

    /**
     * 当前子栈是否还有可以返回的页面。
     *
     * 顶层路由之间的切换不属于系统返回行为，因此不能用 [startKey]
     * 判断这里是否可返回。
     */
    val canGoBack: Boolean
        get() = currentSubStack.size > 1
}

/**
 * 将 NavigationState 转换为 NavEntries。
 */
@Composable
fun NavigationState.toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = subStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider,
        )
    }

    return topLevelStack
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}
