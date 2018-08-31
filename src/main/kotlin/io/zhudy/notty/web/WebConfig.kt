package io.zhudy.notty.web

import io.zhudy.notty.web.v1.TaskResource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.router

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
@Configuration
class WebConfig(
        private val taskResource: TaskResource
) {

    @Bean
    fun mainRouter() = router {
        path("/api/v1").nest {
            POST("/tasks", taskResource::newTask)
        }
    }

}