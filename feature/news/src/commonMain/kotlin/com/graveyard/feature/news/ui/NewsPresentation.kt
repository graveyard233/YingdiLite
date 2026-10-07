package com.graveyard.feature.news.ui

import androidx.compose.runtime.Composable
import com.graveyard.core.data.result.DataError
import com.graveyard.core.data.result.DataException
import org.jetbrains.compose.resources.stringResource
import yingdilite.feature.news.generated.resources.Res
import yingdilite.feature.news.generated.resources.news_error_business
import yingdilite.feature.news.generated.resources.news_error_generic
import yingdilite.feature.news.generated.resources.news_error_http
import yingdilite.feature.news.generated.resources.news_error_network
import yingdilite.feature.news.generated.resources.news_error_parsing
import yingdilite.feature.news.generated.resources.news_error_timeout
import yingdilite.feature.news.generated.resources.news_reply_count_large
import yingdilite.feature.news.generated.resources.news_time_days_ago
import yingdilite.feature.news.generated.resources.news_time_hours_ago
import yingdilite.feature.news.generated.resources.news_time_minutes_ago
import yingdilite.feature.news.generated.resources.news_time_just_now

@Composable
internal fun relativeNewsTime(epochSeconds: Long, nowEpochSeconds: Long): String? {
    if (epochSeconds <= 0 || epochSeconds > nowEpochSeconds) return null
    val elapsed = nowEpochSeconds - epochSeconds
    return when {
        elapsed < 60 -> stringResource(Res.string.news_time_just_now)
        elapsed < 3_600 -> stringResource(
            Res.string.news_time_minutes_ago,
            (elapsed / 60).toInt(),
        )
        elapsed < 86_400 -> stringResource(
            Res.string.news_time_hours_ago,
            (elapsed / 3_600).toInt(),
        )
        else -> stringResource(Res.string.news_time_days_ago, (elapsed / 86_400).toInt())
    }
}

@Composable
internal fun commentCount(raw: String): String? {
    val count = raw.toLongOrNull()?.takeIf { it >= 0 } ?: return null
    return if (count < 10_000) {
        count.toString()
    } else {
        // 中文按“万”取整，英文资源用 "%1$d0k+" 表达同等的向下取整语义
        stringResource(Res.string.news_reply_count_large, (count / 10_000).toInt())
    }
}

@Composable
internal fun Throwable.newsErrorMessage(): String =
    (this as? DataException)?.error?.newsErrorMessage()
        ?: stringResource(Res.string.news_error_generic)

@Composable
internal fun DataError.newsErrorMessage(): String = when (this) {
    DataError.Network -> stringResource(Res.string.news_error_network)
    DataError.Timeout -> stringResource(Res.string.news_error_timeout)
    is DataError.Http -> stringResource(Res.string.news_error_http)
    is DataError.Business -> stringResource(Res.string.news_error_business)
    is DataError.Parsing -> stringResource(Res.string.news_error_parsing)
    DataError.Unknown -> stringResource(Res.string.news_error_generic)
}
