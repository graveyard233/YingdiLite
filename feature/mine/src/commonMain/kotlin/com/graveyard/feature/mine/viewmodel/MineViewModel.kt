package com.graveyard.feature.mine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.data.repository.AuthRepository
import com.graveyard.core.data.result.DataResult
import com.graveyard.feature.mine.auth.MineLoginCoordinator
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class MineViewModel(
    private val authRepository: AuthRepository,
    loginCoordinator: MineLoginCoordinator,
) : ViewModel() {
    private val isLoggingOut = MutableStateFlow(false)
    private val dailyTasks = MutableStateFlow(DailyTasksUiState())
    private val eventChannel = Channel<MineUiEvent>(Channel.BUFFERED)

    /** 每日任务所属账号；与它不一致的请求结果直接丢弃。 */
    private var dailyTasksUserId: Long? = null
    private var dailyTasksJob: Job? = null
    private var dailyTasksLoadedAt: TimeMark? = null

    val uiState = combine(
        authRepository.state,
        isLoggingOut,
        loginCoordinator.state,
        dailyTasks,
    ) { authState, loggingOut, login, tasks ->
        MineUiState(
            authState = authState,
            isLoggingOut = loggingOut,
            isLoggingIn = login.isSubmitting,
            dailyTasks = tasks,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MineUiState(
            authState = authRepository.state.value,
            isLoggingIn = loginCoordinator.state.value.isSubmitting,
        ),
    )
    val events = eventChannel.receiveAsFlow()

    init {
        // 登录态建立（冷启动恢复或新登录）或换号时重新加载；登出时清空。
        viewModelScope.launch {
            authRepository.state
                .map { (it as? AuthState.Authenticated)?.userId }
                .distinctUntilChanged()
                .collect { userId ->
                    dailyTasksJob?.cancel()
                    dailyTasksJob = null
                    dailyTasksLoadedAt = null
                    dailyTasksUserId = userId
                    dailyTasks.value = DailyTasksUiState()
                    if (userId != null) refreshDailyTasks(force = true)
                }
        }
    }

    fun logout() {
        if (isLoggingOut.value) return
        isLoggingOut.value = true
        viewModelScope.launch {
            try {
                when (authRepository.logout()) {
                    is DataResult.Success -> Unit
                    is DataResult.Failure -> eventChannel.send(MineUiEvent.LogoutFailed)
                }
            } finally {
                isLoggingOut.value = false
            }
        }
    }

    /**
     * 页面回到前台时调用；距上次成功加载不足 [DAILY_TASKS_THROTTLE] 时跳过，[force] 忽略节流。
     * 任务在其他页面完成，所以靠回到本页时刷新来同步，跨天也由此更新。
     */
    fun refreshDailyTasks(force: Boolean = false) {
        val userId = dailyTasksUserId ?: return
        if (dailyTasksJob?.isActive == true) return
        if (!force && dailyTasksLoadedAt?.let { it.elapsedNow() < DAILY_TASKS_THROTTLE } == true) return
        dailyTasks.update { it.copy(isLoading = true, loadFailed = false) }
        dailyTasksJob = viewModelScope.launch {
            val result = authRepository.getDailyTasks()
            if (dailyTasksUserId != userId) return@launch
            when (result) {
                is DataResult.Success -> {
                    val previous = dailyTasks.value.tasks
                    dailyTasks.value = DailyTasksUiState(tasks = result.data)
                    dailyTasksLoadedAt = TimeSource.Monotonic.markNow()
                    // 有任务刚完成说明经验值或营火变了，顺带刷新资料让上方余额同步。
                    val hasNewlyCompleted = previous != null && result.data.any { task ->
                        task.completed && previous.none { it.id == task.id && it.completed }
                    }
                    if (hasNewlyCompleted) launch { authRepository.refreshCurrentUser() }
                }
                is DataResult.Failure -> dailyTasks.update {
                    it.copy(isLoading = false, loadFailed = it.tasks == null)
                }
            }
        }
    }

    fun retryDailyTasks() = refreshDailyTasks(force = true)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        val DAILY_TASKS_THROTTLE = 30.seconds
    }
}
