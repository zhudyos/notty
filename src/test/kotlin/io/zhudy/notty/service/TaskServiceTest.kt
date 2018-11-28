package io.zhudy.notty.service

import io.zhudy.notty.HttpMockServer
import org.springframework.web.reactive.function.client.WebClient
import org.testng.annotations.Test

/**
 * @author Kevin Zou (yong.zou@2339.com)
 */
class TaskServiceTest : HttpMockServer() {

    @Test
    fun newTask() {
        val wc = WebClient.create()

        val s = wc.get()
                .uri("$baseUri/hello")
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
        println(s)
    }

}