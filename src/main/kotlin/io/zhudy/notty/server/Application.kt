package io.zhudy.notty.server

import com.mongodb.reactivestreams.client.MongoClients
import io.lettuce.core.RedisClient
import io.zhudy.notty.NottyProps
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer

/**
 *
 * @author Kevin Zou (kevinz@weghst.com)
 */
@SpringBootApplication(exclude = [
    MongoReactiveAutoConfiguration::class,
    DataSourceAutoConfiguration::class,
    RestTemplateAutoConfiguration::class,
    ErrorWebFluxAutoConfiguration::class,
    CodecsAutoConfiguration::class,
    PersistenceExceptionTranslationAutoConfiguration::class,
    TransactionAutoConfiguration::class,
    ValidationAutoConfiguration::class
])
@ComponentScan("io.zhudy.notty")
class Application {

    @Bean
    fun kotlinPropertyConfigurer() = PropertySourcesPlaceholderConfigurer().apply {
        setPlaceholderPrefix("%{")
        setTrimValues(true)
        setIgnoreUnresolvablePlaceholders(true)
    }

    @Bean
    @ConfigurationProperties("notty")
    fun nottyProps() = NottyProps()

    @Bean
    fun mongoClient(@Value("%{notty.mongodb.uri}") uri: String) = MongoClients.create(uri)!!

    @Bean(destroyMethod = "shutdown")
    fun redisClient(@Value("%{notty.redis.uri}") uri: String) = RedisClient.create(uri)!!

    @Bean(destroyMethod = "close")
    fun redisPub(redisClient: RedisClient) = redisClient.connectPubSub()!!

    @Bean(destroyMethod = "close")
    fun redisSub(redisClient: RedisClient) = redisClient.connectPubSub()!!

    @Bean(destroyMethod = "close")
    fun redisConn(redisClient: RedisClient) = redisClient.connect()!!

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<Application>(*args) {
            }
        }
    }
}