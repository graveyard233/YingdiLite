package com.graveyard.core.data.repository

import com.graveyard.core.data.auth.AuthState
import com.graveyard.core.data.auth.AuthUser
import com.graveyard.core.data.result.DataResult
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    /** 不暴露 token；首次读盘完成前为 Restoring。 */
    val state: StateFlow<AuthState>

    suspend fun loginWithPhone(countryCode: String, phone: String, password: String): DataResult<Unit>
    suspend fun loginWithAccount(account: String, password: String): DataResult<Unit>
    suspend fun logout(): DataResult<Unit>
    suspend fun refreshCurrentUser(): DataResult<AuthUser>
}
