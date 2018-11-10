package io.zhudy.notty.service

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.zhudy.kitty.biz.BizCodeException
import io.zhudy.kitty.domain.Pageable
import io.zhudy.notty.BizCodes
import io.zhudy.notty.RedisKeys
import io.zhudy.notty.domain.Task
import io.zhudy.notty.domain.TaskStatus
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
        val nowCall = cbDelayMs <= 0

        return redisConn.reactive()
                .zadd(RedisKeys.TASK_QUEUE, score.toDouble(), id)
                .flatMap { taskRepository.insert(task) }
                .doOnNext {
                    if (nowCall) {
                        // 如果任务本身没有延迟则立即在当前进程中执行回调任务
                        // 反之则
                        notificationService.invoke(task)
                                .doOnSuccess { tcl ->
                                    log.info("回调成功: {}", tcl)
                                }
                                .doOnError { e ->
                                    log.error("回调任务失败: {}", id, e)
                                }
                                .subscribe()
                    }
                }
                .flatMap {
                    redisPub.reactive().publish(NotificationService.NEW_TASK_CHANNEL, id)
                }
                .map { id }
    }

    /**
     * 执行指定的任务。
     *
     * @param id 任务ID
     */
    @Suppress("HasPlatformType")
    fun invoke(id: String) = taskRepository.findById(id).flatMap(notificationService::invoke)

    /**
     * 取消指定的任务。
     *
     * @param id 任务ID
     */
    @Suppress("HasPlatformType")
    fun cancel(id: String) = taskRepository.findById(id)
            .doOnNext {
                if (it.status != TaskStatus.PROCESSING) {
                    throw BizCodeException(BizCodes.C_4004,
                            "当前任务状态为[${it.status}]，只能取消状态为[${TaskStatus.PROCESSING}的任务")
                }
            }
            .flatMap {
                taskRepository.cancel(id)
            }
            .doOnSuccess {
                redisConn.reactive().zrem(RedisKeys.TASK_QUEUE, id).subscribe()
            }

    /**
     * 根据ID查询指定任务信息。
     */
    @Suppress("HasPlatformType")
    fun findById(id: String) = taskRepository.findById(id)

    /**
     * 查询任务。
     */
    @Suppress("HasPlatformType")
    fun findTasks(pageable: Pageable) = taskRepository.findTasks(pageable)

    /**
     * 查询任务回调记录。
     */
    @Suppress("HasPlatformType")
    fun findLogsById(id: String, pageable: Pageable) = taskRepository.findLogsById(id, pageable)

}