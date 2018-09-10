package io.zhudy.notty.web.v1

import io.zhudy.notty.service.TaskService
import io.zhudy.notty.vo.NewTaskVo
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
@Component
class TaskResource(
        private val taskService: TaskService
) {

    /**
     * 创建新的通知任务。
     */
    fun newTask(request: ServerRequest) = request.bodyToMono(NewTaskVo::class.java)
            .flatMap { taskService.newTask(it) }
            .flatMap { ok().body(Mono.just(mapOf("id" to it))) }

    /**
     * 查询任务列表。
     */
    fun findTasks(request: ServerRequest) = ok().build()

    /**
     * 取消通知任务。
     */
    fun cancel(request: ServerRequest) = ok().build()

    /**
     * 根据任务ID查询任务信息。
     */
    fun findById(request: ServerRequest) = ok().build()

    /**
     * 根据任务ID查询任务回调日志信息。
     */
    fun findLogsById(request: ServerRequest) = ok().build()
}