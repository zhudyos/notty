package io.zhudy.notty.dto

/**
 * 创建任务返回结果。
 *
 * @property tid 任务ID
 * @property dids 数据IDs
 *
 * @author Kevin Zou (kevinz@weghst.com)
 */
data class NewTaskResultDto(
        val tid: String,
        val dids: List<String>
)