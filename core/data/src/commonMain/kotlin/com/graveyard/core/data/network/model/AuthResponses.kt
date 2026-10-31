package com.graveyard.core.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RefreshLoginResponse(@SerialName("login_token") val loginToken: String) {
    override fun toString(): String = "RefreshLoginResponse(token=<redacted>)"
}

/** `POST /web/user/user-detail` 的身份展示字段；敏感及暂未使用的资料不解析、不缓存。 */
@Serializable
internal data class UserDetailResponse(
    @SerialName("user_id") val userId: Long,
    val nickname: String = "",
    val avatar: String = "",
    @SerialName("background_image") val backgroundImage: String = "",
    @SerialName("personal_remark") val personalRemark: String = "",
    @SerialName("credits_score") val creditsScore: Long = 0L,
    @SerialName("fire_coin") val fireCoin: Long = 0L,
    @SerialName("post_total") val postTotal: Long = 0L,
    @SerialName("follower") val follower: Long = 0L,
    @SerialName("following") val following: Long = 0L,
    @SerialName("like_total") val likeTotal: Long = 0L,
    @SerialName("iphome") val ipHome: String = "",
)
