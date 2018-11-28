package io.zhudy.notty.service

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.netty.channel.ChannelOption
import io.zhudy.notty.RedisKeys
import io.zhudy.notty.domain.CbMethod
import io.zhudy.notty.domain.Task
import io.zhudy.notty.domain.TaskCallLog
import io.zhudy.notty.domain.TaskStatus
import io.zhudy.notty.repository.TaskRepository
import kotlinx.atomicfu.atomic
import org.slf4j.LoggerFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpClient
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.concurrent.thread

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
@Service
class NotificationService(
        private val redisClient: RedisClient,
        private val redisConn: StatefulRedisConnection<String, String>,
        private val redisSub: StatefulRedisPubSubConnection<String, String>,
        private val taskRepository: TaskRepository
) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    companion object {
        const val NEW_TASK_CHANNEL = "notty:new:task"
        const val TASK_MIN_DELAY_MS: Long = 5 * 1000
    }

    private val webClient = WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(
                    HttpClient.from(TcpClient.create()
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000))
            ))
            .build()

    private val taskLock = ReentrantLock()
    private val taskCond = taskLock.newCondition()
    private val shutdownCond = taskLock.newCondition()

    private val perTaskMax = Runtime.getRuntime().availableProcessors()
    private val minDelayMs = 5 * 1000
    private var done = true

    private val pullTaskScriptSha = {
        val os = NotificationService::class.java.getResourceAsStream("/redis/pull_task.lua")
        val script = String(os.readBytes())
        val comm = redisConn.sync()
        val sha = comm.digest(script)
        if (!comm.scriptExists(sha).first()) {
            comm.scriptLoad(script)
        }
        sha
    }.invoke()

    @PostConstruct
    fun init() {
        thread(start = true, name = "notification-service", block = ::execute)
        listenNewTask()
    }

    @PreDestroy
    fun destroy() {
        done = false
        try {
            taskLock.lock()
            taskCond.signal()
            shutdownCond.await()
        } finally {
            taskLock.unlock()
        }
    }

    private fun execute() {
        val conn = redisClient.connect()
        val comms = conn.sync()
        val minDelayMsStr = minDelayMs.toString()
        val concTaskMax = 5000
        val currConcTask = atomic(0)

        val lock = ReentrantLock()
        val cond = lock.newCondition()

        var limit = perTaskMax
        while (done) {
            try {
                val taskIds = comms.evalsha<List<String>>(
                        pullTaskScriptSha,
                        ScriptOutputType.VALUE,
                        arrayOf(RedisKeys.TASK_QUEUE),
                        System.currentTimeMillis().toString(),
                        limit.toString(),
                        minDelayMsStr
                )
                currConcTask.addAndGet(taskIds.size)

                taskIds.forEach {
                    Mono.just(it)
                            .flatMap(::findById)
                            .flatMap(::invoke)
                            .doOnSuccessOrError { _, _ ->
                                currConcTask.decrementAndGet()

                                // FIXME 锁可继续优化
                                try {
                                    lock.lock()
                                    cond.signal()
                                } finally {
                                    lock.unlock()
                                }
                            }
                            .subscribe()
                }

                /*
                 * 如果当前处理的任务数已超过最大任务处理数，则等待部分任务处理完成，在接受新的任务。
                 * 保证同时并发的任务在可控范围之内。
                 */
                while (currConcTask.value >= concTaskMax) {
                    try {
                        lock.lock()
                        cond.await()
                    } catch (e: InterruptedException) {
                        // ignore
                    } finally {
                        lock.unlock()
                    }
                }

                if (taskIds.size < limit) {
                    val sv = comms.zrangeWithScores(RedisKeys.TASK_QUEUE, 0, 0).firstOrNull()
                    val waitSec = if (sv == null) 30000L else (sv.score - System.currentTimeMillis()).toLong()

                    try {
                        taskLock.lock()
                        taskCond.await(waitSec, TimeUnit.MILLISECONDS)
                    } catch (e: InterruptedException) {
                        // ignore
                    } finally {
                        taskLock.unlock()
                    }
                }

                limit = concTaskMax - currConcTask.value
                if (limit > perTaskMax) {
                    limit = perTaskMax
                }
            } catch (e: RedisException) {
                log.error("{}", e)
                if (!conn.isOpen) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            } catch (e: Exception) {
                log.error("{}", e)
            }
        }

        try {
            taskLock.lock()
            conn.close()
            shutdownCond.signal()
        } finally {
            taskLock.unlock()
        }
    }

    private fun findById(id: String) = taskRepository.findById(id)

    /**
     * 执行回调任务。
     */
    fun invoke(task: Task): Mono<TaskCallLog> {
        val req = when (task.cbMethod) {
            CbMethod.GET -> webClient.get()
            CbMethod.POST -> {
                val p = webClient.post()
                if (task.cbData != null) {
                    if (task.cbContentType.isNotEmpty()) {
                        p.header("content-type", task.cbContentType)
                    } else {
                        p.header("content-type", "application/json; charset=utf-8")
                    }
                    p.body(task.cbData.toMono())
                }
                p
            }
        }

        return req.uri(task.cbUrl)
                .header("x-request-id", task.id)
                .exchange()
                .zipWhen { it.bodyToMono(String::class.java) }
                .flatMap {
                    val res = it.t1
                    val status = res.statusCode()
                    val log = TaskCallLog(
                            taskId = task.id,
                            n = task.retryCount + 1,
                            httpResHeaders = res.headers().asHttpHeaders().toString(),
                            httpResStatus = status.value(),
                            httpResBody = it.t2 ?: "",
                            success = status.is2xxSuccessful,
                            createdAt = System.currentTimeMillis()
                    )

                    taskRepository.succeed(task.id, TaskStatus.SUCCEEDED, log).map { log }
                }
                .onErrorResume { t ->
                    val reason = if (t is UnknownHostException) {
                        t.message
                    } else {
                        t.message
                    } ?: t.javaClass.canonicalName

                    val log = TaskCallLog(
                            taskId = task.id,
                            n = task.retryCount + 1,
                            success = false,
                            reason = reason,
                            createdAt = System.currentTimeMillis()
                    )

                    taskRepository.fail(task.id, TaskStatus.FAILED, log).map { log }
                }.map { log ->
                    val comm = redisConn.reactive()
                    // 归档任务不在通知
                    if (log.success || log.n >= task.retryMaxCount) {
                        comm.zrem(RedisKeys.TASK_QUEUE, task.id).subscribe()
                    } else {
                        // 增加延迟时间
                        val nextMs = log.n * log.n * 1000L
                        if (nextMs > minDelayMs) {
                            comm.zaddincr(
                                    RedisKeys.TASK_QUEUE,
                                    (nextMs - minDelayMs).toDouble(),
                                    task.id
                            ).subscribe()
                        }
                    }

                    log
                }
    }

    private fun listenNewTask() {
        val c = redisSub.reactive()
        redisSub.addListener(object : RedisPubSubAdapter<String, String>() {

            override fun message(channel: String, message: String) {
                if (channel == NEW_TASK_CHANNEL) {
                    try {
                        taskLock.lock()
                        taskCond.signal()
                    } finally {
                        taskLock.unlock()
                    }
                }
            }
        })
        c.subscribe(NEW_TASK_CHANNEL).subscribe()
    }
}