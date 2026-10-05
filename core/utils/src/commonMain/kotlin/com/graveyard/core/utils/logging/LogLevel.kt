package com.graveyard.core.utils.logging

/**
 * 日志级别，最低为 DEBUG（无 VERBOSE）。
 */
public enum class LogLevel(public val priority: Int) {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
}
