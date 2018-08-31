package io.zhudy.notty.repository

/**
 * 未找到任务。
 *
 * @author Kevin Zou (kevinz@weghst.com)
 */
class NotFoundTaskException(message: String) : RuntimeException(message) {
}