package io.zhudy.notty.domain

/**
 * 回调通知任务。
 *
 * @property id 任务ID
 * @property serviceName 提交任务的服务名称
 * @property cbUrl 回调地址
 * @property cbMethod 回调方法 [CbMethod]
 * @property cbContentType 回调请求头 `content-type`
 * @property cbData 回调数据
 * @property cbDelay 首次回调延迟时间（毫秒）
 * @property retryCount 已重试次数
 * @property retryMaxCount 最大重试次数
 * @property status 任务状态 [TaskStatus]
 * @property lastCallAt 上次回调时间戳
 * @property succeededAt 成功时间戳
 * @property createdAt 任务创建时间戳
 *
 * @author Kevin Zou (yong.zou@2339.com)
 */
data class Task(
        val id: String,
        val serviceName: String,
        val cbUrl: String,
        val cbMethod: CbMethod,
        val cbContentType: String,
        val cbData: Any?,
        val cbDelay: Long,
        val retryCount: Int,
        val retryMaxCount: Int,
        val status: TaskStatus,
        val lastCallAt: Long,
        val succeededAt: Long,
        val createdAt: Long
)