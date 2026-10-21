package com.graveyard.core.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户主页信息接口响应体（`POST /web/user/user-info`）。
 *
 * 该接口返回的是**平铺对象**，没有 `list` 之类的数组字段，
 * 因此解析时不能当作数组列表处理（见 `YingdiNetData.getUserInfo` 的校验方式）。
 *
 * 字段命名与接口保持一致；接口新增字段时靠 `ignoreUnknownKeys = true` 兜住。
 */
@Serializable
internal data class UserInfoResponse(
    /** 用户 id，与请求参数 `user_id` 相同。 */
    @SerialName("user_id")
    val userId: Long = 0L,

    /** 昵称。 */
    @SerialName("nickname")
    val nickname: String = "",

    /** 头像 URL。 */
    @SerialName("avatar")
    val avatar: String = "",

    /** 主页背景图 URL，未设置时是站方默认背景。 */
    @SerialName("background_image")
    val backgroundImage: String = "",

    /** 个人签名 / 简介。 */
    @SerialName("personal_remark")
    val personalRemark: String = "",

    /** 经验值（积分），可配合 `core:model` 的等级表换算等级。 */
    @SerialName("credits_score")
    val creditsScore: Long = 0L,

    /** 发帖数（帖子/文章总量，口径以服务端为准）。 */
    @SerialName("post_total")
    val postTotal: Long = 0L,

    /** 粉丝数。 */
    @SerialName("follower")
    val follower: Long = 0L,

    /** 关注数。 */
    @SerialName("following")
    val following: Long = 0L,

    /** 累计获赞数。 */
    @SerialName("like_total")
    val likeTotal: Long = 0L,

    /**
     * IP 归属地文案，如 `"浙江"`；可能为空串或 `"未知地点"`，展示时建议隐藏。
     */
    @SerialName("iphome")
    val ipHome: String = "",

    /**
     * 账号状态。`0` 为正常；样本中未出现其他取值，
     * 非 `0` 时建议按「受限/封禁」处理，具体含义需结合封禁接口确认。
     */
    @SerialName("status")
    val status: Int = 0,

    /**
     * 封禁结束时间，Unix 秒级时间戳；`0` 表示未被封禁。
     * 注意它不是毫秒，也不是「封禁时长」。
     */
    @SerialName("banned_time")
    val bannedTime: Long = 0L,

    /** 当前佩戴的勋章、头像边框、昵称颜色。 */
    @SerialName("in_use_medals")
    val inUseMedals: UserMedals = UserMedals(),

    /**
     * 绑定的第三方游戏账号。同一游戏（同一 [SocialAccount.type]）可能出现多条，
     * 展示时请保留全部，不要按游戏名去重（样本里炉石就有 4 条）。
     */
    @SerialName("social_accounts")
    val socialAccounts: List<SocialAccount>? = null,
)

/**
 * 用户装饰信息，对应 `in_use_medals`。
 *
 * 结构与评论接口 `user.badge_json` 二次解析后的 [UserBadgeInfo] 相似，
 * 但字段名不同（这里是 `name_color`，那边是 `color`），因此单独建模。
 */
@Serializable
internal data class UserMedals(
    /** 已获得的勋章图片 URL 列表，可能为空数组。 */
    @SerialName("badges")
    val badges: List<String> = emptyList(),

    /** 头像边框图片 URL，为空串表示使用默认边框。 */
    @SerialName("border")
    val border: String = "",

    /** 昵称颜色（十六进制色值），为空串表示使用默认颜色。 */
    @SerialName("name_color")
    val nameColor: String = "",
)

/**
 * 一条第三方游戏账号绑定信息。
 *
 * 示例：`{"account":"Bennidge#1493","img":"...","social_name":"炉石传说","type":1}`。
 */
@Serializable
internal data class SocialAccount(
    /** 玩家在该游戏内的账号标识，可能包含备注文字（如「（国服）」）。 */
    @SerialName("account")
    val account: String = "",

    /** 游戏图标 URL。 */
    @SerialName("img")
    val img: String = "",

    /** 游戏名称，如「炉石传说」「守望先锋」。适合直接展示，但不适合当唯一键。 */
    @SerialName("social_name")
    val socialName: String = "",

    /**
     * 游戏/平台类型。样本中 `1` = 炉石传说、`2` = 守望先锋；
     * 新游戏上线会新增取值，UI 侧建议用 `socialName` 展示、`type` 仅作图标兜底。
     */
    @SerialName("type")
    val type: Int = 0,
)
