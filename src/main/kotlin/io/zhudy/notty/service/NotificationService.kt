package io.zhudy.notty.service

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.zhudy.notty.RedisKeys
import io.zhudy.notty.domain.CbMethod
import io.zhudy.notty.domain.Task
import io.zhudy.notty.domain.TaskCallLog
import io.zhudy.notty.repository.NotFoundTaskException
import io.zhudy.notty.repository.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.net.UnknownHostException
import java.time.Instant
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
        private val redisPubSub: StatefulRedisPubSubConnection<String, String>,
        private val taskRepository: TaskRepository
) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    companion object {
        const val NEW_TASK_CHANNEL = "notty:new:task"
    }

    private val webClient = WebClient.create()
    private var done = true
    private val lock = ReentrantLock()
    private val newTaskCond = lock.newCondition()
    private val shutdownCond = lock.newCondition()

    private val perTaskMax = 5
    private val minDelaySec = 5

    private val pullTaskScriptSha by lazy {
        val os = NotificationService::class.java.getResourceAsStream("/redis/pull_task.lua")
        val script = String(os.readBytes())
        val comm = redisConn.sync()
        val sha = comm.digest(script)
        if (!comm.scriptExists(sha).first()) {
            comm.scriptLoad(script)
        }
        sha
    }

    @PostConstruct
    fun init() {
        thread(start = true, name = "notification-service", block = ::execute)
        subscribeNewTask()
    }

    @PreDestroy
    fun destroy() {
        done = false
        try {
            lock.lock()
            shutdownCond.await()
        } finally {
            lock.unlock()
        }
    }

    private fun execute() {
        val conn = redisClient.connect()
        val limitStr = perTaskMax.toString()
        val minDelaySecStr = minDelaySec.toString()
        while (done) {
            try {
                val ids = conn.reactive().evalsha<String>(
                        pullTaskScriptSha,
                        ScriptOutputType.VALUE,
                        arrayOf(RedisKeys.TASK_QUEUE),
                        Instant.now().epochSecond.toString(),
                        limitStr,
                        minDelaySecStr)
                        .collectList()
                        .block()

                ids.forEach(::processTask)

                if (ids.size < perTaskMax) {
                    val sv = redisConn.sync().zrangeWithScores(RedisKeys.TASK_QUEUE, 0, 0).firstOrNull()
                    val waitSec = if (sv == null) {
                        30
                    } else {
                        (sv.score - Instant.now().epochSecond).toLong()
                    }

                    try {
                        lock.lock()
                        newTaskCond.await(waitSec, TimeUnit.SECONDS)
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
        findById(id).zipWhen { callRemote(it) }.flatMap {
            val task = it.t1
            val log = it.t2

            if (log.success) {
                taskRepository.succeed(id, log)
            } else {
                taskRepository.fail(id, log)
            }.doOnSuccess {
                // 归档任务不在通知
                if (log.success || log.n >= task.retryMaxCount) {
                    comm.zrem(RedisKeys.TASK_QUEUE, id).subscribe()
                } else {
                    // 增加延迟时间
                    val sec = log.n * log.n
                    val nextSec = if (sec < 10) 10 else sec

                    comm.zaddincr(
                            RedisKeys.TASK_QUEUE,
                            (nextSec - minDelaySec).toDouble(),
                            id
                    ).subscribe()
                }
            }
        }.doOnError { e ->
            if (e is NotFoundTaskException) {
                // 移除任务
                comm.zrem(RedisKeys.TASK_QUEUE, id).subscribe()
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
                .header("x-notty-taskid", task.id)
                .exchange()
                .zipWhen { it.bodyToMono(String::class.java) }
                .map {
                    val res = it.t1
                    val status = res.statusCode()
                    TaskCallLog(
                            taskId = task.id,
                            n = task.retryCount + 1,
                            httpHeaders = res.headers().asHttpHeaders().toString(),
                            httpStatus = status.value(),
                            httpBody = it.t2 ?: "",
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

    private fun subscribeNewTask() {
        val c = redisPubSub.reactive()
        redisPubSub.addListener(object : RedisPubSubAdapter<String, String>() {

            override fun message(channel: String, message: String) {
                if (channel == NEW_TASK_CHANNEL) {
                    try {
                        lock.lock()
                        newTaskCond.signal()
                    } finally {
                        lock.unlock()
                    }
                }
            }
        })
        c.subscribe(NEW_TASK_CHANNEL).subscribe()
    }
}