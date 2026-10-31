package com.graveyard.core.data.auth

import kotlinx.serialization.Serializable

/** 可供页面展示和离线缓存的基础资料，不包含凭证。 */
@Serializable
data class AuthUser(
    val userId: Long,
    val nickname: String,
    val avatar: String,
    val backgroundImage: String = "",
    val personalRemark: String = "",
    val creditsScore: Long = 0L,
    val fireCoin: Long = 0L,
    val postTotal: Long = 0L,
    val follower: Long = 0L,
    val following: Long = 0L,
    val likeTotal: Long = 0L,
    val ipHome: String = "",
)

/** 仅供数据层持久化与请求使用；本期按普通 Preferences 保存。 */
data class AuthSession(
    val loginToken: String,
    val refreshToken: String,
    val userId: Long,
    val user: AuthUser? = null,
) {
    override fun toString(): String = "AuthSession(userId=$userId, tokens=<redacted>)"
}

sealed interface AuthState {
    data object Restoring : AuthState
    data object Guest : AuthState
    data class Authenticated(val userId: Long, val user: AuthUser? = null) : AuthState
}
