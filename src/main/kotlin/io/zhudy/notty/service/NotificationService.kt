package io.zhudy.notty.service

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.zhudy.notty.RedisKeys
import io.zhudy.notty.domain.CbMethod
import io.zhudy.notty.domain.Task
import io.zhudy.notty.domain.TaskCallLog
import io.zhudy.notty.domain.TaskStatus
import io.zhudy.notty.repository.NotFoundTaskException
import io.zhudy.notty.repository.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
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
    }

    private val webClient = WebClient.builder()
            .clientConnector(ReactorClientHttpConnector {
                it.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                        .afterNettyContextInit { ctx ->
                            ctx.addHandlerLast(ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                        }
            })
            .build()

    private val lock = ReentrantLock()
    private val taskCond = lock.newCondition()
    private val shutdownCond = lock.newCondition()

    private val perTaskMax = 5
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
            lock.lock()
            taskCond.signal()
            shutdownCond.await()
        } finally {
            lock.unlock()
        }
    }

    private fun execute() {
        val conn = redisClient.connect()
        val limitStr = perTaskMax.toString()
        val minDelayMsStr = minDelayMs.toString()
        while (done) {
            try {
                val ids = conn.reactive().evalsha<String>(
                        pullTaskScriptSha,
                        ScriptOutputType.VALUE,
                        arrayOf(RedisKeys.TASK_QUEUE),
                        System.currentTimeMillis().toString(),
                        limitStr,
                        minDelayMsStr)
                        .collectList()
                        .block()

                ids.forEach(::processTask)

                if (ids.size < perTaskMax) {
                    val sv = redisConn.sync().zrangeWithScores(RedisKeys.TASK_QUEUE, 0, 0).firstOrNull()
                    val waitSec = if (sv == null) {
                        30 * 1000
                    } else {
                        (sv.score - System.currentTimeMillis()).toLong()
                    }

                    try {
                        lock.lock()
                        taskCond.await(waitSec, TimeUnit.MILLISECONDS)
                    } finally {
                        lock.unlock()
                    }
                }
            } catch (e: RedisException) {
                log.error("{}", e)
                if (!conn.isOpen) {
                    try {
                        TimeUnit.SECONDS.sleep(1)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            } catch (e: Exception) {
                log.error("{}", e)
            }
        }

        conn.close()

        try {
            lock.lock()
            shutdownCond.signal()
        } finally {
            lock.unlock()
        }
    }

    private fun processTask(id: String) {
        val comm = redisConn.reactive()
        findById(id).zipWhen(::callRemote).flatMap {
            val task = it.t1
            val log = it.t2

            if (log.success) {
                taskRepository.succeed(id, TaskStatus.SUCCEEDED, log)
            } else {
                val status = if (log.n >= task.retryMaxCount) TaskStatus.FAILED else TaskStatus.PROCESSING
                taskRepository.fail(id, status, log)
            }.doOnSuccess {
                // 归档任务不在通知
                if (log.success || log.n >= task.retryMaxCount) {
                    comm.zrem(RedisKeys.TASK_QUEUE, id).subscribe()
                } else {
                    // 增加延迟时间
                    val nextMs = log.n * log.n * 1000L
                    if (nextMs > minDelayMs) {
                        comm.zaddincr(
                                RedisKeys.TASK_QUEUE,
                                (nextMs - minDelayMs).toDouble(),
                                id
                        ).subscribe()
                    }
                }
            }
        }.doOnError { e ->
            if (e is NotFoundTaskException) {
                // 移除任务
                comm.zrem(RedisKeys.TASK_QUEUE, id).subscribe()
                log.warn("Not found task: {}", id)
            } else {
                log.error("", e)
            }
        }.subscribe()
    }

    // 优先查询从库，如果从库没有查询到则去主库查询，解决新任务主从同步因时间差查询不到任务的场景
    private fun findById(id: String) = taskRepository.findById(id)
            .onErrorResume(NotFoundTaskException::class.java) {
                taskRepository.findById4Primary(id)
            }

    private fun callRemote(task: Task): Mono<TaskCallLog> {
        val client = when (task.cbMethod) {
            CbMethod.GET -> webClient.get()
            CbMethod.POST -> {
                val p = webClient.post()
                if (task.cbData != null) {
                    if (task.cbContentType.isNotEmpty()) {
                        p.header("content-type", task.cbContentType)
                    } else {
                        p.header("content-type", "application/json; charset=utf-8")
                    }
                    p.body(Mono.just(task.cbData))
                }
                p
            }
        }

        return client.uri(task.cbUrl)
                .header("x-request-id", task.id)
                .exchange()
                .zipWhen { it.bodyToMono(String::class.java) }
                .map {
                    val res = it.t1
                    val status = res.statusCode()
                    TaskCallLog(
                            taskId = task.id,
                            n = task.retryCount + 1,
                            httpResHeaders = res.headers().asHttpHeaders().toString(),
                            httpResStatus = status.value(),
                            httpResBody = it.t2 ?: "",
                            success = status.is2xxSuccessful,
                            createdAt = System.currentTimeMillis()
                    )
                }
                .onErrorResume { u ->
                    val reason = if (u is UnknownHostException) {
                        u.message
                    } else {
                        u.message
                    } ?: u.javaClass.canonicalName

                    TaskCallLog(
                            taskId = task.id,
                            n = task.retryCount + 1,
                            success = false,
                            reason = reason,
                            createdAt = System.currentTimeMillis()
                    ).toMono()
                }
    }

    private fun listenNewTask() {
        val c = redisSub.reactive()
        redisSub.addListener(object : RedisPubSubAdapter<String, String>() {

            override fun message(channel: String, message: String) {
                if (channel == NEW_TASK_CHANNEL) {
                    try {
                        lock.lock()
                        taskCond.signal()
                    } finally {
                        lock.unlock()
                    }
                }
            }
        })
        c.subscribe(NEW_TASK_CHANNEL).subscribe()
    }
}