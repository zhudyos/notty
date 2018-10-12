package io.zhudy.notty.service

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.zhudy.kitty.domain.Pageable
import io.zhudy.notty.RedisKeys
import io.zhudy.notty.domain.Task
import io.zhudy.notty.repository.TaskRepository
import io.zhudy.notty.vo.NewTaskVo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
@Service
class TaskService(
        private val taskRepository: TaskRepository,
        private val redisConn: StatefulRedisConnection<String, String>,
        private val redisPub: StatefulRedisPubSubConnection<String, String>
) {

    private val log = LoggerFactory.getLogger(TaskService::class.java)

    /**
     * 创建新的通知回调任务。
     */
    fun newTask(vo: NewTaskVo): Mono<String> {
        val task = Task(
                serviceName = vo.serviceName,
                sid = vo.sid,
                cbUrl = vo.cbUrl,
                cbMethod = vo.cbMethod,
                cbContentType = vo.cbContentType,
                cbData = vo.cbData,
                cbDelay = vo.cbDelay,
                retryCount = 0,
                retryMaxCount = vo.retryMaxCount
        )

        return taskRepository.insert(task).flatMap { id ->
            val score = System.currentTimeMillis() + task.cbDelay * 1000 + 50
            redisConn.reactive().zadd(RedisKeys.TASK_QUEUE, score.toDouble(), id)
                    .doFinally {
                        // 发布通知处理任务
                        redisPub.reactive()
                                .publish(NotificationService.NEW_TASK_CHANNEL, id)
                                .doOnError {
                                    log.error("taskId: {}", id, it)
                                }
                                .subscribe()
                    }
                    .map { id }
        }
    }

    /**
     * 取消回调任务。
     */
    fun cancel(id: String) = taskRepository.cancel(id)

    /**
     * 根据ID查询指定任务信息。
     */
    fun findById(id: String) = taskRepository.findById(id)

    /**
     * 查询任务。
     */
    fun findTasks(pageable: Pageable) = taskRepository.findTasks(pageable)

    /**
     * 查询任务回调记录。
     */
    fun findLogsById(id: String, pageable: Pageable) = taskRepository.findLogsById(id, pageable)

}