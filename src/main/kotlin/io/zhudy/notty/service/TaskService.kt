package io.zhudy.notty.service

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.zhudy.kitty.domain.Pageable
import io.zhudy.notty.RedisKeys
import io.zhudy.notty.domain.Task
import io.zhudy.notty.repository.TaskRepository
import io.zhudy.notty.vo.NewTaskVo
import org.bson.types.ObjectId
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
        private val redisPub: StatefulRedisPubSubConnection<String, String>,
        private val notificationService: NotificationService
) {

    private val log = LoggerFactory.getLogger(TaskService::class.java)

    /**
     * 创建新的通知回调任务。
     */
    fun newTask(vo: NewTaskVo): Mono<String> {
        val id = ObjectId().toString()
        val task = Task(
                id = id,
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

        val cbDelayMs = task.cbDelay * 1000
        val minDelayMs = if (cbDelayMs > NotificationService.TASK_MIN_DELAY_MS) {
            cbDelayMs
        } else {
            NotificationService.TASK_MIN_DELAY_MS
        }
        val score = System.currentTimeMillis() + minDelayMs

        return redisConn.reactive()
                .zadd(RedisKeys.TASK_QUEUE, score.toDouble(), id)
                .flatMap { taskRepository.insert(task) }
                .flatMap {
                    if (cbDelayMs > 0) {
                        // 如果任务本身没有延迟则立即在当前进程中执行回调任务
                        // 反之则
                        notificationService.invoke(task)
                    } else {
                        // FIXME empty 无法在继续消费了
                        Mono.empty()
                    }
                }.flatMap {
                    redisPub.reactive().publish(NotificationService.NEW_TASK_CHANNEL, id)
                }
                .map { id }
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