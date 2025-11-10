package com.opengnosis.gateway.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsContributor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.server.ServerWebExchange

@Configuration
class MetricsConfig {

    @Bean
    fun customTagsContributor(): WebFluxTagsContributor {
        return WebFluxTagsContributor { exchange, _ ->
            val tags = mutableListOf<Tag>()
            
            // Add user ID tag if available
            exchange.request.headers.getFirst("X-User-Id")?.let {
                tags.add(Tag.of("user_id", it))
            }
            
            // Add service tag based on path
            val path = exchange.request.path.value()
            val service = when {
                path.startsWith("/api/v1/auth") || path.startsWith("/api/v1/users") -> "iam"
                path.startsWith("/api/v1/schools") || path.startsWith("/api/v1/classes") || path.startsWith("/api/v1/subjects") -> "structure"
                path.startsWith("/api/v1/schedules") -> "scheduler"
                path.startsWith("/api/v1/commands") -> "journal-command"
                path.startsWith("/api/v1/students") || path.startsWith("/api/v1/teachers") || path.startsWith("/api/v1/analytics") -> "analytics-query"
                path.startsWith("/api/v1/notifications") -> "notifier"
                else -> "unknown"
            }
            tags.add(Tag.of("backend_service", service))
            
            tags
        }
    }
}
