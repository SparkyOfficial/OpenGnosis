package com.opengnosis.common.exception

import java.time.Instant

data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val traceId: String? = null,
    val details: Map<String, Any>? = null
)

class BusinessException(message: String, val details: Map<String, Any>? = null) : RuntimeException(message)

class ValidationException(message: String, val field: String, val rejectedValue: Any?) : RuntimeException(message)

class ResourceNotFoundException(message: String) : RuntimeException(message)

class ConflictException(message: String, val details: Map<String, Any>? = null) : RuntimeException(message)
