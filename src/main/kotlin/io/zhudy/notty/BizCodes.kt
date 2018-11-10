package io.zhudy.notty

import io.zhudy.kitty.biz.BizCode

/**
 * 业务码。
 *
 * @author Kevin Zou (kevinz@weghst.com)
 */
enum class BizCodes(override val code: Int, override val msg: String, override val status: Int = 400) : BizCode {

    C_4004(4004, "任务状态不符合预期"),
    C_4005(4005, "取消任务失败"),

}