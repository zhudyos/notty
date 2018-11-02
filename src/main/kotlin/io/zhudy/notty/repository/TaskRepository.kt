package io.zhudy.notty.repository

import com.mongodb.ReadPreference
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes.ascending
import com.mongodb.client.model.Indexes.descending
import com.mongodb.client.model.Updates.*
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoCollection
import io.zhudy.kitty.biz.BizCodeException
import io.zhudy.kitty.domain.Pageable
import io.zhudy.notty.BizCodes
import io.zhudy.notty.domain.CbMethod
import io.zhudy.notty.domain.Task
import io.zhudy.notty.domain.TaskCallLog
import io.zhudy.notty.domain.TaskStatus
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.time.format.DateTimeFormatter

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
@Repository
class TaskRepository(
        private val mongoClient: MongoClient
) {

    private val db get() = mongoClient.getDatabase("notty")
    private val taskColl get() = db.getCollection("task")
    private val taskCallLogColl get() = db.getCollection("task_call_log")
    private val dtf = DateTimeFormatter.ofPattern("yyyyMMdd")

    init {
        val indexes = listOf(
                IndexModel(ascending("service_name"), IndexOptions().background(true)),
                IndexModel(ascending("sid"), IndexOptions().background(true)),
                IndexModel(ascending("cb_url"), IndexOptions().background(true)),
                IndexModel(descending("created_at"), IndexOptions().background(true))
        )
        taskColl.createIndexes(indexes).toMono().subscribe()

        taskCallLogColl.createIndexes(listOf(
                IndexModel(ascending("task_id"), IndexOptions().background(true)),
                IndexModel(ascending("created_at"), IndexOptions().background(true))
        )).toMono().subscribe()
    }

    /**
     * 保存新任务。
     */
    fun insert(task: Task): Mono<String> {
        val doc = Document(
                mapOf(
                        "_id" to task.id,
                        "service_name" to task.serviceName,
                        "sid" to task.sid,
                        "cb_url" to task.cbUrl,
                        "cb_method" to task.cbMethod.name,
                        "cb_content_type" to task.cbContentType,
                        "cb_data" to task.cbData,
                        "cb_delay" to task.cbDelay,
                        "retry_count" to task.retryCount,
                        "retry_max_count" to task.retryMaxCount,
                        "status" to TaskStatus.PROCESSING.status,
                        "created_at" to System.currentTimeMillis()
                )
        )

        return taskColl.insertOne(doc).toMono().map { task.id }
    }

    /**
     * 取消任务回调。
     */
    fun cancel(id: String): Mono<Unit> {
        return taskColl.updateOne(
                and(
                        eq("_id", id),
                        eq("status", TaskStatus.PROCESSING.status)
                ),
                set("status", TaskStatus.CANCEL.status)
        ).toMono().flatMap {
            if (it.isModifiedCountAvailable) {
                Mono.error(BizCodeException(BizCodes.C_4005))
            } else {
                Mono.just(Unit)
            }
        }
    }

    /**
     * 根据ID查询任务。
     *
     * @throws NotFoundTaskException 指定ID的任务不存在时
     */
    fun findById(id: String) = findById(taskColl, id)

    /**
     * 在主库根据ID查询任务。
     *
     * @throws NotFoundTaskException 指定ID的任务不存在时
     */
    fun findById4Primary(id: String) = findById(taskColl.withReadPreference(ReadPreference.primary()), id)

    /**
     * 失败的通知任务。
     *
     * @param id 任务ID
     * @param status 更新的任务状态
     * @param taskCallLog 回调日志
     */
    fun fail(id: String, status: TaskStatus, taskCallLog: TaskCallLog) = updateTask(id, status, taskCallLog)

    /**
     * 成功的通知任务。
     *
     * @param id 任务ID
     * @param status 更新的任务状态
     * @param taskCallLog 回调日志
     */
    fun succeed(id: String, status: TaskStatus, taskCallLog: TaskCallLog) = updateTask(id, status, taskCallLog)

    /**
     * 查询任务。
     */
    fun findTasks(pageable: Pageable) = taskColl.find().skip(pageable.offset).limit(pageable.size)
            .toFlux()
            .map(::mapToTask)

    /**
     * 查询任务回调记录。
     */
    fun findLogsById(id: String, pageable: Pageable) = taskCallLogColl.find(eq("task_id", id)).skip(pageable.offset)
            .limit(pageable.size)
            .toFlux()
            .map(::mapToTaskCallLog)

    private fun updateTask(id: String, status: TaskStatus, taskCallLog: TaskCallLog) = taskColl.updateOne(
            eq("_id", id),
            combine(
                    set("status", status.status),
                    set("last_call_at", System.currentTimeMillis()),
                    inc("retry_count", 1)
            )
    ).toMono().flatMap {
        taskCallLogColl.insertOne(
                Document(mapOf(
                        "_id" to ObjectId().toString(),
                        "task_id" to taskCallLog.taskId,
                        "n" to taskCallLog.n,
                        "http_res_status" to taskCallLog.httpResStatus,
                        "http_res_headers" to taskCallLog.httpResHeaders,
                        "http_res_body" to taskCallLog.httpResBody,
                        "created_at" to taskCallLog.createdAt
                ))
        ).toMono()
    }!!

    private fun findById(coll: MongoCollection<Document>, id: String) = coll.find(eq("_id", id))
            .first()
            .toMono()
            .switchIfEmpty(Mono.defer {
                throw NotFoundTaskException(id)
            })
            .map(::mapToTask)

    private fun mapToTask(doc: Document) = Task(
            id = doc.getString("_id"),
            sid = doc.getString("sid") ?: "",
            serviceName = doc.getString("service_name"),
            cbUrl = doc.getString("cb_url"),
            cbMethod = CbMethod.valueOf(doc.getString("cb_method")),
            cbContentType = doc.getString("content-type") ?: "",
            cbData = doc["cb_data"],
            cbDelay = doc.getLong("cb_delay") ?: 0,
            retryCount = doc.getInteger("retry_count", 0),
            retryMaxCount = doc.getInteger("retry_max_count", -1),
            status = TaskStatus.forStatus(doc.getInteger("status")),
            lastCallAt = doc.getLong("last_call_at") ?: 0,
            succeededAt = doc.getLong("succeeded_at") ?: 0,
            createdAt = doc.getLong("created_at")
    )

    private fun mapToTaskCallLog(doc: Document) = TaskCallLog(
            taskId = doc.getString("task_id"),
            n = doc.getInteger("n"),
            httpResStatus = doc.getInteger("http_res_status"),
            httpResHeaders = doc.getString("http_res_headers"),
            httpResBody = doc.getString("http_res_body"),
            success = doc.getBoolean("success"),
            reason = doc.getString("reason"),
            createdAt = doc.getLong("created_at")
    )
}