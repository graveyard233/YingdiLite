package com.graveyard.core.model.news

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class NewsAuthor(
    val id: Long = 0L,
    val username: String = "",
    val head: String = "",
    val credits: Long = 0L,
    val badgeJson: String = "",
    // HTML can supply a level without credits; 0 means the level is unavailable.
    @Transient
    val level: Int = calculateUserLevel(credits),
)

// Preserve the source table's duplicate final threshold.
private val USER_LEVEL_THRESHOLDS = longArrayOf(
    25, 60, 110, 175, 255, 355, 475, 615, 775, 975,
    1193, 1429, 1683, 1955, 2245, 2553, 2879, 3223, 3585, 3965,
    4365, 4785, 5225, 5685, 6165, 6665, 7185, 7725, 8285, 8865,
    9465, 10085, 10725, 11385, 12065, 12765, 13485, 14225, 14985, 15765,
    16565, 17385, 18225, 19085, 19965, 20865, 21785, 22725, 23685, 24665,
    25665, 26685, 27725, 28785, 29865, 30965, 32085, 33225, 34385, 35565,
    36765, 37985, 39225, 40485, 41765, 43065, 44385, 45725, 47085, 48465,
    49865, 51285, 52725, 54185, 55665, 57165, 58685, 60225, 61785, 63365,
    64965, 66585, 68225, 69885, 71565, 73265, 74985, 76725, 78485, 80265,
    82065, 83885, 85725, 87585, 89465, 91365, 93285, 95225, 97185, 97185,
)

private fun calculateUserLevel(credits: Long): Int {
    if (credits >= USER_LEVEL_THRESHOLDS.last()) return USER_LEVEL_THRESHOLDS.size
    return USER_LEVEL_THRESHOLDS.indexOfFirst { credits < it } + 1
}
