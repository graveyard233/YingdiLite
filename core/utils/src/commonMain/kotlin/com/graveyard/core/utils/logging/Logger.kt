package com.graveyard.core.utils.logging

/**
 * 双端统一日志工具，可供任意模块调用。
 *
 * 用法：
 * ```
 * private const val TAG = "NewsRepository"
 * Logger.i(TAG, "load finished")
 * Logger.e(TAG, "request failed", exception)
 * ```
 *
 * 默认 [minLevel] 为 [LogLevel.INFO]：release 包仍输出 INFO/WARN/ERROR，仅过滤 DEBUG。
 * Debug 包在应用启动时调用一次 [setDebug] 即可打开 DEBUG。
 */
public object Logger {

    /** 总开关，关闭后不输出任何日志。 */
    public var enabled: Boolean = true

    /** 低于该级别的日志不输出。 */
    public var minLevel: LogLevel = LogLevel.INFO

    /**
     * 应用启动时配置：debug 包输出 DEBUG 及以上，release 包输出 INFO 及以上。
     */
    public fun setDebug(debug: Boolean) {
        minLevel = if (debug) LogLevel.DEBUG else LogLevel.INFO
    }

    public fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }

    public fun i(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.INFO, tag, message, throwable)
    }

    public fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }

    public fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (!enabled || level.priority < minLevel.priority) return
        try {
            platformLog(level, tag, message, throwable)
        } catch (_: Throwable) {
            // 日志为辅助能力，任何异常都不应影响业务流程。
        }
    }
}
