package com.opengnosis.gateway.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

@Configuration
class RateLimitConfig {

    @Bean
    fun userKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            // Use user ID from JWT token for rate limiting
            val userId = exchange.request.headers.getFirst("X-User-Id")
            Mono.just(userId ?: exchange.request.remoteAddress?.address?.hostAddress ?: "anonymous")
        }
    }
}
