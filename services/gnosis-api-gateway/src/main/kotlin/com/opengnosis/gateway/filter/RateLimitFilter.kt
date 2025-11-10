package com.opengnosis.gateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

@Component
class RateLimitFilter(
    private val redisTemplate: ReactiveStringRedisTemplate
) : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(RateLimitFilter::class.java)

    companion object {
        private const val RATE_LIMIT_PER_MINUTE = 100
        private const val WINDOW_SIZE_SECONDS = 60L
        private val PUBLIC_PATHS = listOf(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/actuator/health",
            "/actuator/prometheus"
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.path.value()

        // Skip rate limiting for public paths
        if (PUBLIC_PATHS.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        val userId = request.headers.getFirst("X-User-Id") ?: "anonymous"
        val key = "rate_limit:$userId"
        val now = Instant.now().epochSecond

        return redisTemplate.opsForZSet()
            .removeRangeByScore(key, 0.0, (now - WINDOW_SIZE_SECONDS).toDouble())
            .then(redisTemplate.opsForZSet().count(key, 0.0, Double.MAX_VALUE))
            .flatMap { count ->
                if (count >= RATE_LIMIT_PER_MINUTE) {
                    logger.warn("Rate limit exceeded for user: $userId, path: $path")
                    rateLimitExceeded(exchange, count.toInt())
                } else {
                    redisTemplate.opsForZSet()
                        .add(key, now.toString(), now.toDouble())
                        .then(redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SIZE_SECONDS)))
                        .then(Mono.defer {
                            // Add rate limit headers
                            val response = exchange.response
                            response.headers.add("X-RateLimit-Limit", RATE_LIMIT_PER_MINUTE.toString())
                            response.headers.add("X-RateLimit-Remaining", (RATE_LIMIT_PER_MINUTE - count - 1).toString())
                            response.headers.add("X-RateLimit-Reset", (now + WINDOW_SIZE_SECONDS).toString())
                            
                            chain.filter(exchange)
                        })
                }
            }
            .onErrorResume { e ->
                logger.error("Error checking rate limit for user: $userId", e)
                // On Redis error, allow the request to proceed
                chain.filter(exchange)
            }
    }

    private fun rateLimitExceeded(exchange: ServerWebExchange, currentCount: Int): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.TOO_MANY_REQUESTS
        response.headers.add("Content-Type", "application/json")
        response.headers.add("X-RateLimit-Limit", RATE_LIMIT_PER_MINUTE.toString())
        response.headers.add("X-RateLimit-Remaining", "0")
        response.headers.add("X-RateLimit-Reset", (Instant.now().epochSecond + WINDOW_SIZE_SECONDS).toString())
        
        val errorBody = """
            {
                "timestamp": "${Instant.now()}",
                "status": 429,
                "error": "Too Many Requests",
                "message": "Rate limit exceeded. Maximum $RATE_LIMIT_PER_MINUTE requests per minute allowed.",
                "path": "${exchange.request.path.value()}"
            }
        """.trimIndent()
        
        val buffer = response.bufferFactory().wrap(errorBody.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    override fun getOrder(): Int = -50 // Execute after authentication filter
}
