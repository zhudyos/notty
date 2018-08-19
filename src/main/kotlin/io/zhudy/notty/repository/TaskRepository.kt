package io.zhudy.notty.repository

import com.mongodb.ReadPreference
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates.*
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoCollection
import io.zhudy.notty.domain.CbMethod
import io.zhudy.notty.domain.Task
import io.zhudy.notty.domain.TaskCallLog
import io.zhudy.notty.domain.TaskStatus
import org.bson.Document
import org.bson.types.ObjectId
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono

/**
 * @author Kevin Zou (yong.zou@2339.com)
 */
class TaskRepository(
        private val mongoClient: MongoClient
) {

    private val db get() = mongoClient.getDatabase("notty")
    private val taskColl get() = db.getCollection("task")
    private val taskFailColl get() = db.getCollection("task_fail")
    private val taskSuccessColl get() = db.getCollection("task_success")
    private val taskCallLogColl get() = db.getCollection("task_call_log")

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
     * @param taskCallLog 回调日志
     */
    fun fail(id: String, taskCallLog: TaskCallLog) = updateCall(id, taskCallLog, taskFailColl)

    /**
     * 成功的通知任务。
     *
     * @param id 任务ID
     */
    fun succeed(id: String, taskCallLog: TaskCallLog) = updateCall(id, taskCallLog, taskSuccessColl)

    private fun updateCall(id: String, taskCallLog: TaskCallLog, archiveColl: MongoCollection<Document>) = mongoClient
            .startSession()
            .toMono()
            .flatMap { session ->
                session.startTransaction()

                val q = eq("_id", id)
                val m1 = taskColl.findOneAndUpdate(
                        session,
                        q,
                        combine(
                                inc("retry_count", 1),
                                set("last_call_at", System.currentTimeMillis())
                        ),
                        FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
                ).toMono().flatMap { doc ->
                    // 归档
                    val retryCount = doc.getInteger("retry_count", -1)
                    val retryMaxCount = doc.getInteger("retry_max_count", -1)
                    if (retryCount >= retryMaxCount) {
                        archiveColl.insertOne(session, doc).toMono()
                    } else {
                        Mono.just(doc)
                    }
                }

                val m2 = taskColl.deleteOne(q).toMono()

                // 回调日志
                val m3 = taskCallLogColl.insertOne(
                        session,
                        Document(mapOf(
                                "_id" to ObjectId().toString(),
                                "task_id" to taskCallLog.taskId,
                                "n" to taskCallLog.n,
                                "http_status" to taskCallLog.httpStatus,
                                "http_headers" to taskCallLog.httpHeaders,
                                "http_body" to taskCallLog.httpBody,
                                "created_at" to taskCallLog.createdAt
                        ))
                ).toMono()

                Mono.`when`(m1, m2, m3).doOnSuccess {
                    session.commitTransaction()
                }.doOnError {
                    session.abortTransaction()
                }
            }

    private fun findById(coll: MongoCollection<Document>, id: String) = coll.find(eq("_id", id))
            .first()
            .toMono()
            .switchIfEmpty(Mono.create {
                it.error(throw NotFoundTaskException(id))
            })
            .map {
                Task(
                        id = id,
                        serviceName = it.getString("service_name"),
                        cbUrl = it.getString("cb_url"),
                        cbMethod = CbMethod.valueOf(it.getString("cb_method")),
                        cbContentType = it.getString("content-type") ?: "",
                        cbData = it["cb_data"],
                        cbDelay = it.getLong("cb_delay") ?: 0,
                        retryCount = it.getInteger("retry_count", 0),
                        retryMaxCount = it.getInteger("retry_max_count", -1),
                        status = TaskStatus.forStatus(it.getInteger("status")),
                        lastCallAt = it.getLong("last_call_at") ?: 0,
                        succeededAt = it.getLong("succeeded_at") ?: 0,
                        createdAt = it.getLong("created_at")
                )
            }!!
}