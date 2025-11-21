package com.opengnosis.common.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Servlet filter that ensures all requests have a trace ID in the logging context.
 * If the request already has a trace ID header (from upstream services), it will be used.
 * Otherwise, a new trace ID will be generated.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class LoggingFilter : OncePerRequestFilter() {
    
    private val logger = LoggerFactory.getLogger(LoggingFilter::class.java)
    
    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val SPAN_ID_HEADER = "X-Span-Id"
        const val REQUEST_ID_HEADER = "X-Request-Id"
    }
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // Extract or generate trace ID
            val traceId = request.getHeader(TRACE_ID_HEADER) ?: LoggingContext.ensureTraceId()
            LoggingContext.setTraceId(traceId)
            
            // Extract span ID if present
            request.getHeader(SPAN_ID_HEADER)?.let { spanId ->
                LoggingContext.setSpanId(spanId)
            }
            
            // Extract or generate request ID
            val requestId = request.getHeader(REQUEST_ID_HEADER) ?: java.util.UUID.randomUUID().toString()
            LoggingContext.setRequestId(requestId)
            
            // Add trace ID to response headers for client tracking
            response.setHeader(TRACE_ID_HEADER, traceId)
            response.setHeader(REQUEST_ID_HEADER, requestId)
            
            // Log the incoming request
            logger.debug(
                "Incoming request: {} {} from {}",
                request.method,
                request.requestURI,
                request.remoteAddr
            )
            
            val startTime = System.currentTimeMillis()
            
            try {
                filterChain.doFilter(request, response)
            } finally {
                val duration = System.currentTimeMillis() - startTime
                
                // Log the completed request
                logger.debug(
                    "Completed request: {} {} - Status: {} - Duration: {}ms",
                    request.method,
                    request.requestURI,
                    response.status,
                    duration
                )
            }
        } finally {
            // Clear the logging context to prevent leakage to other requests
            LoggingContext.clear()
        }
    }
    
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // Don't filter actuator endpoints to reduce noise
        val path = request.requestURI
        return path.startsWith("/actuator/")
    }
}
