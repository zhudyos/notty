package io.zhudy.notty.vo

import com.fasterxml.jackson.annotation.JsonProperty
import io.zhudy.notty.domain.CbMethod

/**
 *
 *
 * @author Kevin Zou (kevinz@weghst.com)
 */
data class NewTaskVo(
        @JsonProperty("service_name")
        val serviceName: String,
        val sid: String,
        @JsonProperty("cb_url")
        val cbUrl: String,
        @JsonProperty("cb_method")
        val cbMethod: CbMethod,
        @JsonProperty("cb_content_type")
        val cbContentType: String = "",
        @JsonProperty("cb_data")
        val cbData: Any?,
        @JsonProperty("cb_delay")
        val cbDelay: Long = 0,
        @JsonProperty("retry_max_count")
        val retryMaxCount: Int = 99
)