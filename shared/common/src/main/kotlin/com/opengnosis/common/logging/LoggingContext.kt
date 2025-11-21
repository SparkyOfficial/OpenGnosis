package com.opengnosis.common.logging

import org.slf4j.MDC
import java.util.*

/**
 * Utility class for managing logging context with trace IDs and other contextual information.
 * This enables correlation of logs across distributed services.
 */
object LoggingContext {
    
    private const val TRACE_ID_KEY = "traceId"
    private const val SPAN_ID_KEY = "spanId"
    private const val USER_ID_KEY = "userId"
    private const val REQUEST_ID_KEY = "requestId"
    
    /**
     * Set the trace ID in the logging context.
     * This should be called at the entry point of each request.
     */
    fun setTraceId(traceId: String) {
        MDC.put(TRACE_ID_KEY, traceId)
    }
    
    /**
     * Get the current trace ID from the logging context.
     */
    fun getTraceId(): String? = MDC.get(TRACE_ID_KEY)
    
    /**
     * Generate and set a new trace ID if one doesn't exist.
     */
    fun ensureTraceId(): String {
        val existingTraceId = getTraceId()
        if (existingTraceId != null) {
            return existingTraceId
        }
        
        val newTraceId = generateTraceId()
        setTraceId(newTraceId)
        return newTraceId
    }
    
    /**
     * Set the span ID in the logging context.
     */
    fun setSpanId(spanId: String) {
        MDC.put(SPAN_ID_KEY, spanId)
    }
    
    /**
     * Get the current span ID from the logging context.
     */
    fun getSpanId(): String? = MDC.get(SPAN_ID_KEY)
    
    /**
     * Set the user ID in the logging context.
     */
    fun setUserId(userId: String) {
        MDC.put(USER_ID_KEY, userId)
    }
    
    /**
     * Get the current user ID from the logging context.
     */
    fun getUserId(): String? = MDC.get(USER_ID_KEY)
    
    /**
     * Set the request ID in the logging context.
     */
    fun setRequestId(requestId: String) {
        MDC.put(REQUEST_ID_KEY, requestId)
    }
    
    /**
     * Get the current request ID from the logging context.
     */
    fun getRequestId(): String? = MDC.get(REQUEST_ID_KEY)
    
    /**
     * Clear all logging context.
     * This should be called at the end of request processing to prevent context leakage.
     */
    fun clear() {
        MDC.clear()
    }
    
    /**
     * Clear a specific key from the logging context.
     */
    fun clear(key: String) {
        MDC.remove(key)
    }
    
    /**
     * Execute a block of code with a specific trace ID, then restore the previous context.
     */
    fun <T> withTraceId(traceId: String, block: () -> T): T {
        val previousTraceId = getTraceId()
        try {
            setTraceId(traceId)
            return block()
        } finally {
            if (previousTraceId != null) {
                setTraceId(previousTraceId)
            } else {
                clear(TRACE_ID_KEY)
            }
        }
    }
    
    /**
     * Execute a block of code with a specific user ID, then restore the previous context.
     */
    fun <T> withUserId(userId: String, block: () -> T): T {
        val previousUserId = getUserId()
        try {
            setUserId(userId)
            return block()
        } finally {
            if (previousUserId != null) {
                setUserId(previousUserId)
            } else {
                clear(USER_ID_KEY)
            }
        }
    }
    
    /**
     * Generate a new trace ID.
     */
    private fun generateTraceId(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }
    
    /**
     * Copy the current logging context to a map.
     * Useful for propagating context to async operations.
     */
    fun copyContext(): Map<String, String> {
        return MDC.getCopyOfContextMap() ?: emptyMap()
    }
    
    /**
     * Restore logging context from a map.
     * Useful for restoring context in async operations.
     */
    fun restoreContext(context: Map<String, String>) {
        MDC.setContextMap(context)
    }
}
