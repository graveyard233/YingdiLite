package com.graveyard.core.model.user

/** 每日任务种类；接口只给 id，按 id 映射，未登记的 id 归为 [Unknown]。 */
enum class DailyTaskKind {
    CheckIn,
    ReadArticle,
    LikeComment,
    PostComment,
    Unknown;

    companion object {
        fun fromId(id: Int): DailyTaskKind = when (id) {
            1 -> CheckIn
            2 -> ReadArticle
            3 -> LikeComment
            4 -> PostComment
            else -> Unknown
        }
    }
}

/**
 * 当天的一项每日任务。
 *
 * @param title 接口标题括号内的部分（如「每日签到」），仅在 [DailyTaskKind.Unknown] 时作为展示兜底。
 * @param credits 完成后获得的经验值。
 * @param campfire 完成后获得的营火，`0` 表示不奖励营火。
 */
data class DailyTask(
    val id: Int,
    val kind: DailyTaskKind,
    val title: String,
    val credits: Int,
    val campfire: Int,
    val completed: Boolean,
)
