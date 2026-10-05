package com.graveyard.core.utils.logging

/**
 * 各端实际日志输出：
 * - Android：android.util.Log
 * - iOS：NSLog
 */
internal expect fun platformLog(
    level: LogLevel,
    tag: String,
    message: String,
    throwable: Throwable?,
)
