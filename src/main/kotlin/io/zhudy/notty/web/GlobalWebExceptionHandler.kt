package io.zhudy.notty.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.zhudy.kitty.biz.BizCodeException
import io.zhudy.kitty.biz.PubBizCodes
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebExceptionHandler
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
@Component
class GlobalWebExceptionHandler(
        private val objectMapper: ObjectMapper
) : WebExceptionHandler {

    private val log = LoggerFactory.getLogger(WebExceptionHandler::class.java)

    override fun handle(exchange: ServerWebExchange, e: Throwable): Mono<Void> {
        val response = exchange.response
        if (response.isCommitted) {
            return Mono.empty()
        }

        var status: Int
        val body = when (e) {
            is BizCodeException -> {
                status = e.bizCode.status
                mapOf(
                        "code" to e.bizCode.code,
                        "message" to if (e.exactMessage.isNotEmpty()) e.exactMessage else e.bizCode.msg
                )
            }
            else -> {
                log.error("unhandled exception", e)
                status = 500
                mapOf(
                        "code" to PubBizCodes.C_500.code,
                        "message" to e.message
                )
            }
        }

        response.statusCode = HttpStatus.resolve(status)
        response.headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
        val buffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(body))
        return response.writeWith(Flux.just(buffer))
        return Mono.empty()
    }
}