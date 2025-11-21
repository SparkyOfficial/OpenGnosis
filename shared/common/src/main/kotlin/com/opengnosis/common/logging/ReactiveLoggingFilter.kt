package com.opengnosis.common.logging

import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.util.context.Context

/**
 * WebFlux filter that ensures all requests have a trace ID in the logging context.
 * This is the reactive equivalent of LoggingFilter for WebFlux applications.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ReactiveLoggingFilter : WebFilter {
    
    private val logger = LoggerFactory.getLogger(ReactiveLoggingFilter::class.java)
    
    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val SPAN_ID_HEADER = "X-Span-Id"
        const val REQUEST_ID_HEADER = "X-Request-Id"
        const val TRACE_ID_CONTEXT_KEY = "traceId"
        const val SPAN_ID_CONTEXT_KEY = "spanId"
        const val REQUEST_ID_CONTEXT_KEY = "requestId"
    }
    
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        
        // Extract or generate trace ID
        val traceId = request.headers.getFirst(TRACE_ID_HEADER) 
            ?: java.util.UUID.randomUUID().toString().replace("-", "")
        
        // Extract span ID if present
        val spanId = request.headers.getFirst(SPAN_ID_HEADER)
        
        // Extract or generate request ID
        val requestId = request.headers.getFirst(REQUEST_ID_HEADER) 
            ?: java.util.UUID.randomUUID().toString()
        
        // Add trace ID to response headers
        exchange.response.headers.set(TRACE_ID_HEADER, traceId)
        exchange.response.headers.set(REQUEST_ID_HEADER, requestId)
        
        // Log the incoming request
        logger.debug(
            "Incoming request: {} {} from {}",
            request.method,
            request.uri.path,
            request.remoteAddress?.address?.hostAddress
        )
        
        val startTime = System.currentTimeMillis()
        
        return chain.filter(exchange)
            .doOnSuccess {
                val duration = System.currentTimeMillis() - startTime
                logger.debug(
                    "Completed request: {} {} - Status: {} - Duration: {}ms",
                    request.method,
                    request.uri.path,
                    exchange.response.statusCode?.value(),
                    duration
                )
            }
            .doOnError { error ->
                val duration = System.currentTimeMillis() - startTime
                logger.error(
                    "Failed request: {} {} - Duration: {}ms - Error: {}",
                    request.method,
                    request.uri.path,
                    duration,
                    error.message,
                    error
                )
            }
            .contextWrite { context ->
                // Add trace information to reactor context
                var newContext = context
                    .put(TRACE_ID_CONTEXT_KEY, traceId)
                    .put(REQUEST_ID_CONTEXT_KEY, requestId)
                
                if (spanId != null) {
                    newContext = newContext.put(SPAN_ID_CONTEXT_KEY, spanId)
                }
                
                // Also set in MDC for logging
                LoggingContext.setTraceId(traceId)
                LoggingContext.setRequestId(requestId)
                spanId?.let { LoggingContext.setSpanId(it) }
                
                newContext
            }
    }
}

/**
 * Extension function to extract trace ID from Reactor context.
 */
fun Context.getTraceId(): String? = getOrDefault(ReactiveLoggingFilter.TRACE_ID_CONTEXT_KEY, null)

/**
 * Extension function to extract span ID from Reactor context.
 */
fun Context.getSpanId(): String? = getOrDefault(ReactiveLoggingFilter.SPAN_ID_CONTEXT_KEY, null)

/**
 * Extension function to extract request ID from Reactor context.
 */
fun Context.getRequestId(): String? = getOrDefault(ReactiveLoggingFilter.REQUEST_ID_CONTEXT_KEY, null)
