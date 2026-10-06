package com.graveyard.feature.news.ui

import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException

internal fun relativeNewsTime(epochSeconds: Long, nowEpochSeconds: Long): String? {
    if (epochSeconds <= 0 || epochSeconds > nowEpochSeconds) return null
    val elapsed = nowEpochSeconds - epochSeconds
    return when {
        elapsed < 60 -> "刚刚"
        elapsed < 3_600 -> "${elapsed / 60}分钟前"
        elapsed < 86_400 -> "${elapsed / 3_600}小时前"
        else -> "${elapsed / 86_400}天前"
    }
}

internal fun commentCount(raw: String): String? =
    raw.toLongOrNull()?.takeIf { it >= 0 }?.let { count ->
        if (count < 10_000) count.toString() else "${count / 10_000}万+"
    }

internal fun Throwable.newsErrorMessage(): String =
    (this as? DataException)?.error?.newsErrorMessage() ?: "暂时无法加载，请稍后重试"

internal fun DataError.newsErrorMessage(): String = when (this) {
    DataError.Network -> "网络连接失败，请检查网络"
    DataError.Timeout -> "请求超时，请稍后重试"
    is DataError.Http -> "服务暂时不可用，请稍后重试"
    is DataError.Business -> "暂时无法获取内容，请稍后重试"
    DataError.Parsing -> "内容暂时无法读取，请稍后重试"
    DataError.Unknown -> "暂时无法加载，请稍后重试"
}
