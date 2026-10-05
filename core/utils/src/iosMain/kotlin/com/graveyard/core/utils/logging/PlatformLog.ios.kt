package com.graveyard.core.utils.logging

import platform.Foundation.NSLog

internal actual fun platformLog(
    level: LogLevel,
    tag: String,
    message: String,
    throwable: Throwable?,
) {
    val levelTag = when (level) {
        LogLevel.DEBUG -> "D"
        LogLevel.INFO -> "I"
        LogLevel.WARN -> "W"
        LogLevel.ERROR -> "E"
    }
    val text = if (throwable != null) {
        "$message\n${throwable.stackTraceToString()}"
    } else {
        message
    }
    // 使用 %@ 作为格式，避免 message 中的 % 被 NSLog 当作格式占位符。
    NSLog("%@", "[$levelTag][$tag] $text")
}
