package io.zhudy.notty.service

import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.RedisPubSubAdapter
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
import java.net.UnknownHostException
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
                        .flatMap(::callRemote)

                // FIXME 时间差
            } catch (e: Exception) {
                log.error("{}", e)
            }
        }
        shutdownCond.signal()
    }

    // 优先查询从库，如果从库没有查询到则去主库查询，解决新任务主从同步因时间差查询不到任务的场景
    private fun findById(id: String) = taskRepository.findById(id)
            .onErrorResume(NotFoundTaskException::class.java) {
                taskRepository.findById4Primary(id)
            }

    private fun callRemote(task: Task) = Mono.create<TaskCallLog> { sink ->
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

        client.uri(task.cbUrl)
                .header("x-notty-taskid", task.id)
                .exchange()
                .zipWhen {
                    it.bodyToMono(String::class.java)
                }.doOnSuccessOrError { t, u ->
                    val res = t.t1
                    val taskCallLog = if (res != null) {
                        val status = res.statusCode()
                        TaskCallLog(
                                taskId = task.id,
                                n = task.retryCount + 1,
                                httpHeaders = res.headers().asHttpHeaders().toString(),
                                httpStatus = status.value(),
                                httpBody = t.t2 ?: "",
                                success = status.is2xxSuccessful,
                                createdAt = System.currentTimeMillis()
                        )
                    } else {
                        // FIXME 错误异常映射
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
                        )
                    }

                    sink.success(taskCallLog)
                }.subscribe()
    }

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