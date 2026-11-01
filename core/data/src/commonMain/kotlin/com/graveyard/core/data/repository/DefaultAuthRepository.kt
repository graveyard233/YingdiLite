package com.graveyard.core.data.repository

import com.graveyard.core.data.auth.AuthSession
import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.data.auth.AuthUser
import com.graveyard.core.data.crypto.YingdiSignProfile
import com.graveyard.core.data.local.preferences.UserSettings
import com.graveyard.core.data.network.YingdiApiExecutor
import com.graveyard.core.data.network.model.DailyTaskInfoResponse
import com.graveyard.core.data.network.model.UserDetailResponse
import com.graveyard.core.data.network.networkData.YingdiAuthRemoteData
import com.graveyard.core.data.network.sign.YingdiSignedForm
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException
import com.graveyard.core.data.result.DataResult
import com.graveyard.core.data.result.toDataException
import com.graveyard.core.model.user.DailyTask
import com.graveyard.core.model.user.DailyTaskKind
import com.graveyard.core.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class DefaultAuthRepository(
    private val remote: YingdiAuthRemoteData,
    private val requests: YingdiApiExecutor,
    private val settings: UserSettings,
    private val scope: CoroutineScope,
) : AuthRepository {
    private val mutableState = MutableStateFlow<AuthState>(AuthState.Restoring)
    override val state = mutableState.asStateFlow()

    init {
        scope.launch {
            try {
                val restored = settings.session.first()
                mutableState.value = restored.toAuthState()
                if (restored != null) scope.launch { refreshCurrentUser() }
                settings.session.collect { mutableState.value = it.toAuthState() }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableState.value = AuthState.Guest
                Logger.e("AuthRepository", "读取本地会话失败", exception)
            }
        }
    }

    override suspend fun loginWithPhone(countryCode: String, phone: String, password: String): DataResult<Unit> =
        login(countryCode.trim().removePrefix("+") + "_" + phone.trim(), password)

    override suspend fun loginWithAccount(account: String, password: String): DataResult<Unit> =
        login(account.trim(), password)

    private suspend fun login(username: String, password: String): DataResult<Unit> = dataResult {
        val response = remote.login(username, password)
        if (response.loginToken.isBlank() || response.refreshToken.isBlank() || response.userID <= 0L) {
            throw DataException(DataError.Parsing)
        }
        val session = AuthSession(response.loginToken, response.refreshToken, response.userID)
        settings.saveSession(session)
        mutableState.value = session.toAuthState()
        scope.launch { refreshCurrentUser() }
    }

    override suspend fun logout(): DataResult<Unit> = dataResult {
        settings.clearSession()
        mutableState.value = AuthState.Guest
    }

    override suspend fun refreshCurrentUser(): DataResult<AuthUser> = dataResult {
        val session = settings.session.first()
            ?: throw DataException(DataError.Business(null, "请先登录"))
        val response = requests.post<UserDetailResponse>(
            "/mweb/user/info",
            YingdiSignedForm.of(profile = YingdiSignProfile.Mweb),
        )
        val currentSession = settings.session.first()
        if (currentSession == null || currentSession.userId != session.userId || response.userId != session.userId) {
            throw DataException(DataError.Parsing)
        }
        val user = AuthUser(
            userId = response.userId,
            nickname = response.nickname,
            avatar = response.avatar,
            backgroundImage = response.backgroundImage,
            personalRemark = response.personalRemark,
            creditsScore = response.creditsScore,
            fireCoin = response.fireCoin,
            postTotal = response.postTotal,
            follower = response.follower,
            following = response.following,
            likeTotal = response.likeTotal,
            ipHome = response.ipHome,
        )
        settings.saveUserProfile(user)
        user
    }

    override suspend fun getDailyTasks(): DataResult<List<DailyTask>> = dataResult {
        val session = settings.session.first()
            ?: throw DataException(DataError.Business(null, "请先登录"))
        // 每日任务走 PC 签名；默认 profile 即 Pc，token 与过期刷新由 YingdiApiExecutor 处理。
        val response = requests.post<DailyTaskInfoResponse>("/web/user/center-info", YingdiSignedForm.of())
        // 请求期间登出或换号时丢弃结果，避免把上一个账号的任务显示给当前账号。
        if (settings.session.first()?.userId != session.userId) throw DataException(DataError.Parsing)
        response.taskInfo
            .filter { it.id > 0 }
            .sortedBy { it.id }
            .map { task ->
                DailyTask(
                    id = task.id,
                    kind = DailyTaskKind.fromId(task.id),
                    // 「收益-每日任务（每日签到）」→「每日签到」；没有括号时保留原文。
                    title = task.title.substringAfter('（').substringBefore('）').trim(),
                    credits = task.credits,
                    campfire = task.campfire,
                    completed = task.completed,
                )
            }
    }

    private fun AuthSession?.toAuthState(): AuthState =
        if (this == null) AuthState.Guest else AuthState.Authenticated(userId, user)

    private suspend fun <T> dataResult(block: suspend () -> T): DataResult<T> = try {
        DataResult.Success(block())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        DataResult.Failure(exception.toDataException().error)
    }
}
