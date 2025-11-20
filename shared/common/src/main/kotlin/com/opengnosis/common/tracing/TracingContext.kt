package com.opengnosis.common.tracing

import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import reactor.core.publisher.Mono
import java.util.*

/**
 * Utility class for managing distributed tracing context.
 * Provides methods to extract, propagate, and manage trace context headers
 * for distributed tracing with Jaeger/Zipkin.
 */
object TracingContext {

    // B3 propagation headers (Zipkin/Jaeger standard)
    const val TRACE_ID_HEADER = "X-B3-TraceId"
    const val SPAN_ID_HEADER = "X-B3-SpanId"
    const val PARENT_SPAN_ID_HEADER = "X-B3-ParentSpanId"
    const val SAMPLED_HEADER = "X-B3-Sampled"
    const val FLAGS_HEADER = "X-B3-Flags"
    
    // Additional correlation headers
    const val REQUEST_ID_HEADER = "X-Request-Id"
    const val CORRELATION_ID_HEADER = "X-Correlation-Id"

    /**
     * Extract trace context from HTTP headers.
     */
    fun extractFromHeaders(headers: HttpHeaders): TraceContextData {
        return TraceContextData(
            traceId = headers.getFirst(TRACE_ID_HEADER),
            spanId = headers.getFirst(SPAN_ID_HEADER),
            parentSpanId = headers.getFirst(PARENT_SPAN_ID_HEADER),
            sampled = headers.getFirst(SAMPLED_HEADER),
            requestId = headers.getFirst(REQUEST_ID_HEADER)
        )
    }

    /**
     * Propagate trace context to HTTP headers.
     */
    fun propagateToHeaders(headers: HttpHeaders, context: TraceContextData) {
        context.traceId?.let { headers.set(TRACE_ID_HEADER, it) }
        context.spanId?.let { headers.set(SPAN_ID_HEADER, it) }
        context.parentSpanId?.let { headers.set(PARENT_SPAN_ID_HEADER, it) }
        context.sampled?.let { headers.set(SAMPLED_HEADER, it) }
        context.requestId?.let { headers.set(REQUEST_ID_HEADER, it) }
        context.traceId?.let { headers.set(CORRELATION_ID_HEADER, it) }
    }

    /**
     * Set trace context in MDC for logging.
     */
    fun setMDC(context: TraceContextData) {
        context.traceId?.let { MDC.put("traceId", it) }
        context.spanId?.let { MDC.put("spanId", it) }
        context.requestId?.let { MDC.put("requestId", it) }
    }

    /**
     * Clear trace context from MDC.
     */
    fun clearMDC() {
        MDC.remove("traceId")
        MDC.remove("spanId")
        MDC.remove("requestId")
    }

    /**
     * Generate a new trace ID.
     */
    fun generateTraceId(): String {
        // Generate 128-bit trace ID (32 hex characters)
        return UUID.randomUUID().toString().replace("-", "") + 
               UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }

    /**
     * Generate a new span ID.
     */
    fun generateSpanId(): String {
        // Generate 64-bit span ID (16 hex characters)
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }

    /**
     * Generate a new request ID.
     */
    fun generateRequestId(): String {
        return UUID.randomUUID().toString()
    }
}

/**
 * Data class to hold trace context information.
 */
data class TraceContextData(
    val traceId: String? = null,
    val spanId: String? = null,
    val parentSpanId: String? = null,
    val sampled: String? = "1",
    val requestId: String? = null
) {
    /**
     * Create a new child span context from this context.
     */
    fun createChildSpan(): TraceContextData {
        return copy(
            parentSpanId = spanId,
            spanId = TracingContext.generateSpanId()
        )
    }

    /**
     * Check if this trace is sampled.
     */
    fun isSampled(): Boolean {
        return sampled == "1"
    }
}

/**
 * WebClient filter to automatically propagate trace context.
 * Add this filter to WebClient instances to ensure trace context is propagated.
 */
@Component
class TracingContextWebClientFilter : ExchangeFilterFunction {

    override fun filter(request: ClientRequest, next: ExchangeFilterFunction.ExchangeFunction): Mono<Void> {
        // Extract trace context from MDC
        val traceId = MDC.get("traceId")
        val spanId = MDC.get("spanId")
        val requestId = MDC.get("requestId")

        // If trace context exists, propagate it
        if (traceId != null) {
            val context = TraceContextData(
                traceId = traceId,
                spanId = TracingContext.generateSpanId(),  // New span for outgoing request
                parentSpanId = spanId,
                sampled = "1",
                requestId = requestId
            )

            val modifiedRequest = ClientRequest.from(request)
                .apply {
                    context.traceId?.let { header(TracingContext.TRACE_ID_HEADER, it) }
                    context.spanId?.let { header(TracingContext.SPAN_ID_HEADER, it) }
                    context.parentSpanId?.let { header(TracingContext.PARENT_SPAN_ID_HEADER, it) }
                    context.sampled?.let { header(TracingContext.SAMPLED_HEADER, it) }
                    context.requestId?.let { header(TracingContext.REQUEST_ID_HEADER, it) }
                    context.traceId?.let { header(TracingContext.CORRELATION_ID_HEADER, it) }
                }
                .build()

            return next.exchange(modifiedRequest)
        }

        return next.exchange(request)
    }
}
