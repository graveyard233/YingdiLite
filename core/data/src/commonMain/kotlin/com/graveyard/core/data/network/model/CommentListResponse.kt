package com.graveyard.core.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 帖子/文章评论区接口的响应体。
 *
 * 顶层结构：
 * ```
 * {
 *   "hot_comments":    [ <评论组>, ... ],
 *   "hot_num":         2,
 *   "normal_comments": [ <评论组>, ... ],
 *   "normal_num":      43
 * }
 * ```
 *
 * 说明：
 * - 响应被拆成「热门」和「普通」两个列表，`hot_num` / `normal_num`
 *   是**总条数**（用于分页判断），不一定等于对应列表当前的 `size`。
 * - 列表字段在为空时接口可能返回 `null`。
 * - 嵌套结构依次为：
 *   `CommentListResponse → CommentGroup → CommentReply → CommentItem / CommentUser`。
 * - 解析时请使用 `Json { ignoreUnknownKeys = true; isLenient = true }`。
 */
@Serializable
internal data class CommentListResponse(
    /** 热门评论列表（主评论 + 其下若干条回复）。接口可能返回 `null`。 */
    @SerialName("hot_comments")
    val hotComments: List<CommentGroup>? = null,

    /** 热门评论总数，用于判断是否还有下一页。 */
    @SerialName("hot_num")
    val hotNum: Int = 0,

    /** 普通（按时间倒序）评论列表。接口可能返回 `null`。 */
    @SerialName("normal_comments")
    val normalComments: List<CommentGroup>? = null,

    /** 普通评论总数，用于判断是否还有下一页。 */
    @SerialName("normal_num")
    val normalNum: Int = 0,
)

/**
 * 一条「主评论 + 其下回复」组成的评论组，对应 `hot_comments` / `normal_comments` 数组中的元素。
 *
 * 一个楼层的完整结构：
 * ```
 * {
 *   "main_comment":    { "comment": {...}, "action": 0, "user": {...} },
 *   "reply_comments":  [ { "comment": {...}, "action": 0, "user": {...} }, ... ] | null,
 *   "is_manager":      false
 * }
 * ```
 */
@Serializable
internal data class CommentGroup(
    /** 主评论（楼层本身），包含评论内容、当前用户的操作状态和作者信息。 */
    @SerialName("main_comment")
    val mainComment: CommentReply = CommentReply(),

    /**
     * 主评论下的回复列表。
     *
     * 接口在没有回复时返回 `null`（而不是空数组），这里保留可空以忠实反映接口语义：
     * `null` 表示「该楼没有展示回复」，空列表表示「有回复字段但为空」。
     * `mainComment.comment.replyNum` 才是该楼回复的真实总数，可能大于此列表长度。
     */
    @SerialName("reply_comments")
    val replyComments: List<CommentReply>? = null,

    /** 主评论作者是否为版主/管理员（决定是否显示「版主」标识）。 */
    @SerialName("is_manager")
    val isManager: Boolean = false,
)

/**
 * 单条评论（既用于主评论，也用于回复），包含三部分：
 * 评论本体 [comment]、当前用户对该评论的操作状态 [action]、评论作者 [user]。
 *
 * ```
 * { "comment": {...}, "action": 0, "user": {...} }
 * ```
 */
@Serializable
internal data class CommentReply(
    /** 评论正文及元数据。 */
    val comment: CommentItem = CommentItem(),

    /**
     * 当前登录用户对该评论的「表态/投票」操作结果，由服务端针对本账号计算。
     *
     * 未登录或不支持表态的场景下为 `0`；具体取值含义（如点赞/点踩/中立）
     * 需结合点赞、点踩接口的约定解释，业务层不应直接把它当作 `like_num`。
     */
    val action: Int = 0,

    /** 评论作者信息。 */
    val user: CommentUser = CommentUser(),
)

/**
 * 评论正文实体，对应 JSON 中的 `comment` 对象。
 *
 * 字段说明：
 * - `imgs`、`at_users_json`、`vote_json`、`source_tags`、`badge_json` 等
 *   在接口中都是 **JSON 或分隔符字符串**，这里统一保留为 `String`，交由业务层按需二次解析，
 *   避免接口格式变化直接导致反序列化失败。
 * - 空字符串表示「无值」，注意与 `null` 的区分。
 */
@Serializable
internal data class CommentItem(
    /** 评论 ID。 */
    val id: Long = 0L,

    /**
     * 评论正文，可能包含 HTML（如 `<br>` 换行）。
     * 渲染前需要做 HTML 解码/清洗。
     */
    val content: String = "",

    /**
     * 评论携带的图片，通常是以英文逗号分隔的 URL 列表，例如
     * `"https://pic.iyingdi.com/...png"`；表情图 Gif 也走这个字段。为空串表示无图。
     */
    val imgs: String = "",

    /** 该评论的回复总数（主评论和回复都会带，回复一般为 0）。 */
    @SerialName("reply_num")
    val replyNum: Int = 0,

    /** 点赞数。 */
    @SerialName("like_num")
    val likeNum: Int = 0,

    /** 点踩/不喜欢的数量。 */
    @SerialName("hate_num")
    val hateNum: Int = 0,

    /** 创建时间，Unix 秒级时间戳（非毫秒）。 */
    val created: Long = 0L,

    /**
     * 被回复者（即这条回复是回复给谁的）的用户 ID。
     * 主评论或非回复场景为 `0`。
     */
    @SerialName("receiver_id")
    val receiverId: Long = 0L,

    /**
     * 被回复者的用户名，用于渲染「回复 @某某」。
     * 无被回复者时为空串。
     */
    @SerialName("receiver_name")
    val receiverName: String = "",

    /** 评论所属内容（帖子/文章）的 ID，与本评论的 [id] 不同。 */
    @SerialName("source_id")
    val sourceId: Long = 0L,

    /**
     * 评论所属内容的类型，用于区分帖子、文章、新闻等。
     * 例如 `13` 表示帖子（随营地业务扩展可能出现其他取值）。
     */
    @SerialName("source_type")
    val sourceType: Int = 0,

    /**
     * 评论中被 @ 的用户列表（JSON 数组字符串），
     * 没有 @ 任何人时接口返回空串 `""`，需要做空串判断后再二次 JSON 解析。
     */
    @SerialName("at_users_json")
    val atUsersJson: String = "",

    /**
     * 评论附带的投票/表态信息（JSON 字符串），如点赞、踩、玩梗等选项及结果。
     * 无投票时为空串，需要二次 JSON 解析。
     */
    @SerialName("vote_json")
    val voteJson: String = "",

    /**
     * 评论所属话题/标签 ID（以英文逗号分隔的字符串），例如 `"17"`。
     * 为空串表示无话题。
     */
    @SerialName("source_tags")
    val sourceTags: String = "",

    /** 评论作者是否已删除该评论（删除后仍可能返回占位内容）。 */
    @SerialName("author_delete")
    val authorDelete: Boolean = false,

    /** 评论是否可见，`1` 可见、`0` 隐藏/已屏蔽。 */
    @SerialName("is_visible")
    val isVisible: Int = 1,

    /**
     * IP 归属地文案，如 `"重庆"`、`"上海"`，也可能为 `"未知地点"`。
     * 展示时建议在值等于「未知地点」或为空时隐藏该行。
     */
    @SerialName("iphome")
    val ipHome: String = "",

    /**
     * Steam 愿望单状态（评论来自 Steam 愿望单相关功能时使用）。
     * `0` 表示未设置/不适用。
     */
    @SerialName("steam_wishlist_status")
    val steamWishlistStatus: Int = 0,
)

/**
 * 评论作者信息，对应评论对象中的 `user`。
 *
 * 与新闻列表中的 [NewsAuthor] 字段基本相同（id / username / head / credits / badge_json），
 * 但这里独立建模，避免评论接口字段变动影响新闻列表解析。
 */
@Serializable
internal data class CommentUser(
    /** 用户 ID。 */
    val id: Long = 0L,

    /** 用户名（展示名）。 */
    val username: String = "",

    /** 头像 URL。 */
    val head: String = "",

    /** 用户积分/经验值，可用于计算等级（见 `core:model` 的 NewsAuthor 等级表）。 */
    val credits: Long = 0L,

    /**
     * 用户勋章/成就、头像边框、昵称颜色等装饰信息的 JSON 字符串，例如
     * `{"badges":["https://..."],"border":"https://...","color":""}`。
     * 需要二次 JSON 解析，可反序列化为 [UserBadgeInfo]。
     */
    @SerialName("badge_json")
    val badgeJson: String = "",
)

/**
 * 对 `user.badge_json` 做二次解析后的结构。
 *
 * 示例：
 * ```
 * {
 *   "badges": ["https://wspic.iyingdi.cn/achievement/badge/icon/ratyear,w_136,h_35.png"],
 *   "border": "https://wspic.iyingdi.cn/user_item/border/ia_100000025.png",
 *   "color":   ""
 * }
 * ```
 */
@Serializable
internal data class UserBadgeInfo(
    /** 用户已获得的勋章图片 URL 列表，可能为空数组。 */
    val badges: List<String> = emptyList(),

    /** 头像边框图片 URL，为空串表示使用默认边框。 */
    val border: String = "",

    /** 昵称颜色（十六进制色值），为空串表示使用默认颜色。 */
    val color: String = "",
)
