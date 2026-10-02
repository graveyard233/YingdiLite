package com.graveyard.core.designsystem.navigation


import androidx.navigation3.runtime.NavKey

/**
 * 通过更新导航状态来处理导航事件（前进和后退）。
 *
 * @param state - 响应导航事件时将被更新的导航状态。
 */
class Navigator(val state: NavigationState) {

    /**
     * 导航到指定的导航键。
     *
     * @param key - 要导航到的导航键。
     */
    fun navigate(key: NavKey) {
        when (key) {
            state.currentTopLevelKey -> Unit
            in state.topLevelKeys -> goToTopLevel(key)
            else -> goToKey(key)
        }
    }

    /**
     * 返回到上一个导航键。
     */
    fun goBack() {
        if (!state.canGoBack) return

        when (state.currentKey) {
            state.currentTopLevelKey -> {
                // 当前位于当前子栈的底部，因此回退到上一个顶层栈。
                state.topLevelStack.removeLastOrNull()
            }
            else -> state.currentSubStack.removeLastOrNull()
        }
    }

    /**
     * 跳转到非顶层键。
     */
    private fun goToKey(key: NavKey) {
        state.currentSubStack.apply {
            // 如果它已经在栈中，先移除，以便将其添加到栈末尾。
            remove(key)
            add(key)
        }
    }

    /**
     * 跳转到顶层栈。
     */
    private fun goToTopLevel(key: NavKey) {
        state.topLevelStack.apply {
            if (key == state.startKey) {
                // 这是起始键。清空栈，使其成为栈中唯一的键。
                clear()
            } else {
                // 如果它已经在栈中，先移除，以便将其添加到栈末尾。
                remove(key)
            }
            add(key)
        }
    }

}
