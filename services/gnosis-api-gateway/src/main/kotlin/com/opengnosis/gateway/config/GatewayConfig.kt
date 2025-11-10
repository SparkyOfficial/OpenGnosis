package com.opengnosis.gateway.config

import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayConfig {

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            // IAM Service routes
            .route("iam-service") { r ->
                r.path("/api/v1/auth/**", "/api/v1/users/**")
                    .uri("http://gnosis-iam:8080")
            }
            // Structure Service routes
            .route("structure-service") { r ->
                r.path("/api/v1/schools/**", "/api/v1/classes/**", "/api/v1/subjects/**")
                    .uri("http://gnosis-structure:8080")
            }
            // Scheduler Service routes
            .route("scheduler-service") { r ->
                r.path("/api/v1/schedules/**")
                    .uri("http://gnosis-scheduler:8080")
            }
            // Journal Command Service routes
            .route("journal-command-service") { r ->
                r.path("/api/v1/commands/**")
                    .uri("http://gnosis-journal-command:8080")
            }
            // Analytics Query Service routes
            .route("analytics-query-service") { r ->
                r.path("/api/v1/students/**", "/api/v1/teachers/**", "/api/v1/analytics/**", "/api/v1/search/**")
                    .uri("http://gnosis-analytics-query:8080")
            }
            // Notifier Service routes
            .route("notifier-service") { r ->
                r.path("/api/v1/notifications/**")
                    .uri("http://gnosis-notifier:8080")
            }
            .build()
    }
}
