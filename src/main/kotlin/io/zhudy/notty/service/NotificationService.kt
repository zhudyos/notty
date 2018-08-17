package io.zhudy.notty.service

import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.zhudy.notty.RedisKeys
import io.zhudy.notty.domain.CbMethod
import io.zhudy.notty.domain.Task
import io.zhudy.notty.repository.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.concurrent.thread

/**
 * @author Kevin Zou (yong.zou@2339.com)
 */
@Service
class NotificationService(
        private val redisClient: RedisClient,
        private val redisConn: StatefulRedisConnection<String, String>,
        private val taskRepository: TaskRepository
) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    private val webClient = WebClient.create()
    private val newTaskChannel = "notty:new:task"
    private var done = true
    private val lock = ReentrantLock()
    private val newTaskCond = lock.newCondition()
    private val shutdownCond = lock.newCondition()
    private val limit = "5"
    private val minDelayMs = TimeUnit.SECONDS.toMillis(5).toString()

    private val job = thread(name = "notification-service", block = ::execute)
    private val pullTaskScriptSha by lazy {
        val os = NotificationService::class.java.getResourceAsStream("/redis/pull_task.lua")
        val script = String(os.readBytes())
        val comms = redisConn.sync()
        val sha1 = comms.digest(script)
        if (!comms.scriptExists(sha1).first()) {
            comms.scriptLoad(script)
        }
        sha1
    }

    @PostConstruct
    fun init() {
        job.start()
        subscribeNewTask()
    }

    @PreDestroy
    fun destroy() {
        done = false
        shutdownCond.await()
    }

    private fun execute() {
        while (done) {
            try {
                redisConn.reactive().evalsha<String>(
                        pullTaskScriptSha,
                        ScriptOutputType.VALUE,
                        arrayOf(RedisKeys.TASK_QUEUE),
                        System.currentTimeMillis().toString(),
                        limit,
                        minDelayMs)
                        .flatMap(::findById)
                        .flatMap {
                            val client = when (it.cbMethod) {
                                CbMethod.GET -> webClient.get()
                                CbMethod.POST -> {
                                    val p = webClient.post()
                                    if (it.cbData != null) {
                                        if (it.cbContentType.isNotEmpty()) {
                                            p.header("content-type", it.cbContentType)
                                        } else {
                                            p.header("content-type", "application/json; charset=utf-8")
                                        }
                                        p.body(Mono.just(it.cbData))
                                    }

                                    p
                                }
                            }

                            client.uri(it.cbUrl)
                                    .header("x-notty-taskid", it.id)
                                    .exchange()
                                    .doOnError {
                                        // 添加日志
                                    }

                            Mono.empty()
                        }

                // FIXME 时间差
            } catch (e: Exception) {
                log.error("{}", e)
            }
        }
        shutdownCond.signal()
    }

    private fun findById(id: String) = taskRepository.findById(id)

//    private fun callErr(task: Task, t: Throwable)

    private fun subscribeNewTask() {
        val pubSub = redisClient.connectPubSub()
        val comms = pubSub.reactive()
        pubSub.addListener(object : RedisPubSubAdapter<String, String>() {

            override fun message(channel: String, message: String) {
                if (channel == newTaskChannel) {
                    newTaskCond.signal()
                }
            }
        })
        comms.subscribe(newTaskChannel).subscribe()
    }
}