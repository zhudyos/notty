package io.zhudy.notty.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.zhudy.notty.web.v1.TaskResource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.router
import java.util.concurrent.TimeUnit

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
@Configuration
@EnableWebFlux
class WebConfig(
        private val objectMapper: ObjectMapper,
        private val taskResource: TaskResource
) : WebFluxConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .maxAge(TimeUnit.DAYS.toSeconds(7))
    }

    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        val defaults = configurer.defaultCodecs()
        defaults.jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
        defaults.jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
    }

    @Bean
    fun mainRouter() = router {
        path("/api/v1").nest {
            POST("/tasks", taskResource::newTask)
            GET("/tasks", taskResource::findTasks)
            DELETE("/tasks/{id}", taskResource::cancel)
            GET("/tasks/{id}", taskResource::findById)
            GET("/tasks/{id}/logs", taskResource::findLogsById)
        }
    }

}