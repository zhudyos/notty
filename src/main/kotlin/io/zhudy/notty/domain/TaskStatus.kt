package io.zhudy.notty.domain

/**
 * 通知任务状态。
 *
 * @property status 状态值
 * @property isEnd 是否为终态
 *
 * @author Kevin Zou (kevinz@weghst.com)
 */
enum class TaskStatus(
        val status: Int,
        val isEnd: Boolean
) {

    /**
     * 进行中的任务。
     */
    PROCESSING(status = 1, isEnd = false),
    /**
     * 成功的任务。
     */
    SUCCEEDED(status = 8, isEnd = true),
    /**
     * 失败的任务。
     */
    FAILED(status = 9, isEnd = true),
    ;

    companion object {

        /**
         * 返回任务状态枚举。
         */
        fun forStatus(status: Int) = values().first { it.status == status }
    }
}