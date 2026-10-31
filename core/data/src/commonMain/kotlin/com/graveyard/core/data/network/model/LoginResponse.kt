package com.graveyard.core.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("login_token")
    val loginToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("user_id")
    val userID: Long
) {
    override fun toString(): String = "LoginResponse(userID=$userID, tokens=<redacted>)"
}
