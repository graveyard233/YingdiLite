package com.graveyard.feature.mine.viewmodel

import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.model.user.DailyTask

data class MineUiState(
    val authState: AuthState = AuthState.Restoring,
    val isLoggingOut: Boolean = false,
    val isLoggingIn: Boolean = false,
    val dailyTasks: DailyTasksUiState = DailyTasksUiState(),
)

/**
 * 每日任务只保存在内存里。[tasks] 为 `null` 表示本账号还没有成功加载过；
 * 已有数据时刷新失败会保留旧数据，[loadFailed] 只在没有数据可展示时为 `true`。
 */
data class DailyTasksUiState(
    val tasks: List<DailyTask>? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
)

enum class MineUiEvent {
    LogoutFailed,
}
