package io.zhudy.notty.ser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.zhudy.notty.domain.TaskStatus
import org.springframework.boot.jackson.JsonComponent

/**
 * [TaskStatus] jackson 序列化。
 *
 * @author Kevin Zou (yong.zou@2339.com)
 */
@JsonComponent
class TaskStatusSerializer : JsonSerializer<TaskStatus>() {

    override fun serialize(value: TaskStatus, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeNumber(value.status)
    }
}