package com.opengnosis.gateway.filter

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

@Component
class RequestLoggingFilter(
    private val meterRegistry: MeterRegistry
) : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val traceId = UUID.randomUUID().toString()
        val startTime = Instant.now()

        // Add trace ID to request headers
        val modifiedRequest = request.mutate()
            .header("X-Trace-Id", traceId)
            .build()

        val modifiedExchange = exchange.mutate().request(modifiedRequest).build()

        // Set trace ID in MDC for logging
        MDC.put("traceId", traceId)

        logger.info(
            "Incoming request: method={}, path={}, remoteAddress={}",
            request.method,
            request.path.value(),
            request.remoteAddress?.address?.hostAddress
        )

        return chain.filter(modifiedExchange)
            .doOnSuccess {
                val duration = java.time.Duration.between(startTime, Instant.now()).toMillis()
                val response = modifiedExchange.response
                val statusCode = response.statusCode?.value() ?: 0

                logger.info(
                    "Request completed: method={}, path={}, status={}, duration={}ms",
                    request.method,
                    request.path.value(),
                    statusCode,
                    duration
                )

                // Record custom metrics
                recordMetrics(request.path.value(), request.method.toString(), statusCode, duration)
            }
            .doOnError { error ->
                val duration = java.time.Duration.between(startTime, Instant.now()).toMillis()
                
                logger.error(
                    "Request failed: method={}, path={}, duration={}ms, error={}",
                    request.method,
                    request.path.value(),
                    duration,
                    error.message,
                    error
                )

                // Record error metrics
                recordMetrics(request.path.value(), request.method.toString(), 500, duration)
            }
            .doFinally {
                MDC.remove("traceId")
            }
    }

    private fun recordMetrics(path: String, method: String, statusCode: Int, durationMs: Long) {
        // Record request latency
        Timer.builder("gateway.request.duration")
            .tag("path", path)
            .tag("method", method)
            .tag("status", statusCode.toString())
            .register(meterRegistry)
            .record(java.time.Duration.ofMillis(durationMs))

        // Record error rate
        if (statusCode >= 400) {
            meterRegistry.counter(
                "gateway.request.errors",
                "path", path,
                "method", method,
                "status", statusCode.toString()
            ).increment()
        }

        // Record request count
        meterRegistry.counter(
            "gateway.request.total",
            "path", path,
            "method", method,
            "status", statusCode.toString()
        ).increment()
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE // Execute first
}
