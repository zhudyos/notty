package io.zhudy.notty

import io.zhudy.kitty.biz.BizCode

/**
 * 业务码。
 *
 * @author Kevin Zou (yong.zou@2339.com)
 */
enum class BizCodes(override val code: Int, override val msg: String, override val status: Int = 400) : BizCode {

    C_4005(4005, "取消任务失败")

}