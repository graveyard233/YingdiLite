package com.graveyard.core.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 个人中心信息接口响应体（`POST /web/user/center-info`，PC 签名，无业务参数），这里只映射每日任务部分。
 *
 * 响应是平铺对象，`task_info` 只是其中一个字段，因此请求时不传 `expectedArrayField`。
 */
@Serializable
internal data class DailyTaskInfoResponse(
    @SerialName("task_info")
    val taskInfo: List<DailyTaskDto> = emptyList(),
)

@Serializable
internal data class DailyTaskDto(
    /** 任务 id：1 签到、2 阅读文章、3 评论点赞、4 发表评论。 */
    @SerialName("id")
    val id: Int = 0,

    /** 形如「收益-每日任务（每日签到）」。 */
    @SerialName("title")
    val title: String = "",

    /** 奖励经验值。 */
    @SerialName("credits")
    val credits: Int = 0,

    /** 奖励营火。 */
    @SerialName("campfire")
    val campfire: Int = 0,

    @SerialName("completed")
    val completed: Boolean = false,
)
