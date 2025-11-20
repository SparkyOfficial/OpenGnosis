package com.opengnosis.common.tracing

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Configuration for distributed tracing support.
 * This configuration should be imported by all services that need tracing support.
 */
@Configuration
class TracingConfiguration(
    private val tracingInterceptor: TracingInterceptor
) : WebMvcConfigurer {

    /**
     * Register the tracing interceptor to extract trace context from incoming requests.
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(tracingInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/actuator/**", "/health", "/metrics")
    }

    /**
     * Create a WebClient bean with tracing context propagation.
     * Services can inject this bean to make HTTP calls with automatic trace propagation.
     */
    @Bean
    fun tracingWebClient(tracingFilter: TracingContextWebClientFilter): WebClient {
        return WebClient.builder()
            .filter(tracingFilter)
            .build()
    }
}
