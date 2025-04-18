@file:Suppress("unused")

package ltd.finelink.read.exception

/**
 * 并发限制
 */
class ConcurrentException(msg: String, val waitTime: Int) : NoStackTraceException(msg)