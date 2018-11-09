package io.zhudy.notty.web.v1

import io.zhudy.kitty.web.pageParam
import io.zhudy.notty.service.TaskService
import io.zhudy.notty.vo.NewTaskVo
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.noContent
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
    @Suppress("HasPlatformType")
    fun newTask(request: ServerRequest) = request.bodyToMono(NewTaskVo::class.java)
            .flatMap(taskService::newTask)
            .flatMap { ok().body(Mono.just(mapOf("id" to it))) }

//    fun invoke(request: ServerRequest)

    /**
     * 取消通知任务。
     */
    fun cancel(request: ServerRequest) = taskService.cancel(
            request.pathVariable("id")
    ).flatMap {
        noContent().build()
    }

    /**
     * 查询任务列表。
     */
    fun findTasks(request: ServerRequest) = ok().body(taskService.findTasks(request.pageParam()))

    /**
     * 根据任务ID查询任务信息。
     */
    fun findById(request: ServerRequest) = ok().body(
            taskService.findById(request.pathVariable("id"))
    )

    /**
     * 根据任务ID查询任务回调日志信息。
     */
    fun findLogsById(request: ServerRequest) = ok().body(taskService.findLogsById(
            request.pathVariable("id"),
            request.pageParam()
    ))
}