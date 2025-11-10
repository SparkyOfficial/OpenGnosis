package com.opengnosis.gateway.filter

import com.opengnosis.common.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private val PUBLIC_PATHS = listOf(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/actuator/prometheus"
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.path.value()

        // Skip authentication for public paths
        if (PUBLIC_PATHS.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        val authHeader = request.headers.getFirst(AUTHORIZATION_HEADER)

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("Missing or invalid Authorization header for path: $path")
            return unauthorized(exchange, "Missing or invalid Authorization header")
        }

        val token = authHeader.substring(BEARER_PREFIX.length)

        return try {
            if (!jwtTokenProvider.validateToken(token)) {
                logger.warn("Invalid JWT token for path: $path")
                return unauthorized(exchange, "Invalid or expired token")
            }

            val claims = jwtTokenProvider.parseToken(token)
            val userId = claims.subject
            val email = claims["email"] as? String ?: ""
            val roles = claims["roles"] as? String ?: ""

            // Add user context to request headers for downstream services
            val modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Email", email)
                .header("X-User-Roles", roles)
                .build()

            val modifiedExchange = exchange.mutate().request(modifiedRequest).build()

            logger.debug("Successfully authenticated user: $userId for path: $path")
            chain.filter(modifiedExchange)
        } catch (e: Exception) {
            logger.error("Error validating JWT token for path: $path", e)
            unauthorized(exchange, "Authentication failed: ${e.message}")
        }
    }

    private fun unauthorized(exchange: ServerWebExchange, message: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.add("Content-Type", "application/json")
        
        val errorBody = """
            {
                "timestamp": "${java.time.Instant.now()}",
                "status": 401,
                "error": "Unauthorized",
                "message": "$message",
                "path": "${exchange.request.path.value()}"
            }
        """.trimIndent()
        
        val buffer = response.bufferFactory().wrap(errorBody.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    override fun getOrder(): Int = -100 // Execute before other filters
}
