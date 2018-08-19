package io.zhudy.notty.domain

/**
 * 任务通知日志。
 *
 * @property taskId 任务ID
 * @property n 第几次通知
 * @property httpStatus 通知返回的 HTTP Status
 * @property httpHeaders 通知返回的 HTTP Headers
 * @property httpBody 返回返回的 HTTP Body
 * @property success 是否通知成功
 * @property reason 结果说明
 * @property createdAt 通知返回时间
 *
 * @author Kevin Zou (yong.zou@2339.com)
 */
data class TaskCallLog(
        val taskId: String,
        val n: Int,
        val httpStatus: Int = 0,
        val httpHeaders: String = "",
        val httpBody: String = "",
        val success: Boolean,
        val reason: String = "",
        val createdAt: Long
)