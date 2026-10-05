package com.graveyard.core.utils.logging

import android.util.Log

private const val MAX_CHUNK_LENGTH = 3500

internal actual fun platformLog(
    level: LogLevel,
    tag: String,
    message: String,
    throwable: Throwable?,
) {
    val priority = when (level) {
        LogLevel.DEBUG -> Log.DEBUG
        LogLevel.INFO -> Log.INFO
        LogLevel.WARN -> Log.WARN
        LogLevel.ERROR -> Log.ERROR
    }
    val text = if (throwable != null) {
        "$message\n${throwable.stackTraceToString()}"
    } else {
        message
    }
    // 按行 + 分块输出，避免 Logcat 单条消息截断。
    text.lineSequence().forEach { line ->
        line.chunked(MAX_CHUNK_LENGTH).forEach { chunk ->
            Log.println(priority, tag, chunk)
        }
    }
}
