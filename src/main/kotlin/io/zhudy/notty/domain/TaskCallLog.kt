package io.zhudy.notty.domain

/**
 * 任务通知日志。
 *
 * @property taskId 任务ID
 * @property n 第几次通知
 * @property httpResStatus 通知响应的 HTTP Status
 * @property httpResHeaders 通知响应的 HTTP Headers
 * @property httpResBody 通知响应的 HTTP Body
 * @property success 是否通知成功
 * @property reason 结果说明
 * @property createdAt 通知返回时间
 *
 * @author Kevin Zou (kevinz@weghst.com)
 */
data class TaskCallLog(
        val taskId: String,
        val n: Int,
        val httpResStatus: Int = 0,
        val httpResHeaders: String = "",
        val httpResBody: String = "",
        val success: Boolean,
        val reason: String = "",
        val createdAt: Long
)