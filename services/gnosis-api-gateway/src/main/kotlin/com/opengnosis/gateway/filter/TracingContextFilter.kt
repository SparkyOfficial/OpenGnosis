package com.opengnosis.gateway.filter

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.*

/**
 * Filter to propagate distributed tracing context headers.
 * This filter ensures that trace context is propagated through the entire request chain
 * for distributed tracing with Jaeger/Zipkin.
 * 
 * Propagated headers:
 * - X-B3-TraceId: Unique identifier for the trace
 * - X-B3-SpanId: Unique identifier for the current span
 * - X-B3-ParentSpanId: Identifier of the parent span
 * - X-B3-Sampled: Whether this trace should be sampled (1 or 0)
 * - X-Request-Id: Unique identifier for the request (for correlation)
 */
@Component
class TracingContextFilter : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(TracingContextFilter::class.java)

    companion object {
        // B3 propagation headers (used by Zipkin/Jaeger)
        const val TRACE_ID_HEADER = "X-B3-TraceId"
        const val SPAN_ID_HEADER = "X-B3-SpanId"
        const val PARENT_SPAN_ID_HEADER = "X-B3-ParentSpanId"
        const val SAMPLED_HEADER = "X-B3-Sampled"
        const val FLAGS_HEADER = "X-B3-Flags"
        
        // Additional correlation headers
        const val REQUEST_ID_HEADER = "X-Request-Id"
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val headers = request.headers

        // Extract or generate trace ID
        val traceId = headers.getFirst(TRACE_ID_HEADER) 
            ?: generateTraceId()
        
        // Extract or generate span ID
        val spanId = headers.getFirst(SPAN_ID_HEADER) 
            ?: generateSpanId()
        
        // Extract parent span ID (if exists)
        val parentSpanId = headers.getFirst(PARENT_SPAN_ID_HEADER)
        
        // Extract or set sampling decision (default to sampled)
        val sampled = headers.getFirst(SAMPLED_HEADER) 
            ?: "1"  // 1 = sampled, 0 = not sampled
        
        // Extract or generate request ID for correlation
        val requestId = headers.getFirst(REQUEST_ID_HEADER) 
            ?: UUID.randomUUID().toString()

        // Build modified request with trace context headers
        val modifiedRequest = request.mutate()
            .header(TRACE_ID_HEADER, traceId)
            .header(SPAN_ID_HEADER, spanId)
            .apply {
                if (parentSpanId != null) {
                    header(PARENT_SPAN_ID_HEADER, parentSpanId)
                }
            }
            .header(SAMPLED_HEADER, sampled)
            .header(REQUEST_ID_HEADER, requestId)
            .header(CORRELATION_ID_HEADER, traceId)  // Use trace ID as correlation ID
            .build()

        val modifiedExchange = exchange.mutate().request(modifiedRequest).build()

        // Set trace context in MDC for logging
        MDC.put("traceId", traceId)
        MDC.put("spanId", spanId)
        MDC.put("requestId", requestId)

        logger.debug(
            "Trace context propagated: traceId={}, spanId={}, parentSpanId={}, sampled={}, requestId={}",
            traceId, spanId, parentSpanId, sampled, requestId
        )

        return chain.filter(modifiedExchange)
            .doFinally {
                // Clean up MDC
                MDC.remove("traceId")
                MDC.remove("spanId")
                MDC.remove("requestId")
            }
    }

    /**
     * Generate a trace ID in the format expected by Zipkin/Jaeger.
     * Format: 16 or 32 hex characters (128-bit or 64-bit)
     */
    private fun generateTraceId(): String {
        // Generate 128-bit trace ID (32 hex characters)
        return UUID.randomUUID().toString().replace("-", "") + 
               UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }

    /**
     * Generate a span ID in the format expected by Zipkin/Jaeger.
     * Format: 16 hex characters (64-bit)
     */
    private fun generateSpanId(): String {
        // Generate 64-bit span ID (16 hex characters)
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE + 1  // Execute after RequestLoggingFilter
}
