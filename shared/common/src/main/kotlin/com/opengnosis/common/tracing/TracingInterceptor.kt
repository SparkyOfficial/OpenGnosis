package com.opengnosis.common.tracing

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Interceptor to extract and propagate trace context for incoming HTTP requests.
 * This interceptor should be registered in the WebMvcConfigurer of each service.
 */
@Component
class TracingInterceptor : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(TracingInterceptor::class.java)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        // Extract trace context from request headers
        val traceId = request.getHeader(TracingContext.TRACE_ID_HEADER) 
            ?: TracingContext.generateTraceId()
        val spanId = request.getHeader(TracingContext.SPAN_ID_HEADER) 
            ?: TracingContext.generateSpanId()
        val parentSpanId = request.getHeader(TracingContext.PARENT_SPAN_ID_HEADER)
        val sampled = request.getHeader(TracingContext.SAMPLED_HEADER) ?: "1"
        val requestId = request.getHeader(TracingContext.REQUEST_ID_HEADER) 
            ?: TracingContext.generateRequestId()

        // Create trace context
        val context = TraceContextData(
            traceId = traceId,
            spanId = spanId,
            parentSpanId = parentSpanId,
            sampled = sampled,
            requestId = requestId
        )

        // Set trace context in MDC for logging
        TracingContext.setMDC(context)

        // Add trace headers to response for debugging
        response.setHeader(TracingContext.TRACE_ID_HEADER, traceId)
        response.setHeader(TracingContext.REQUEST_ID_HEADER, requestId)

        logger.debug(
            "Trace context extracted: traceId={}, spanId={}, parentSpanId={}, requestId={}",
            traceId, spanId, parentSpanId, requestId
        )

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        // Clear MDC after request completion
        TracingContext.clearMDC()
    }
}
