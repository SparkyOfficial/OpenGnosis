package com.opengnosis.analytics.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {
    
    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val cacheConfigurations = mapOf(
            "studentGrades" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)),
            "studentGradesByPeriod" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)),
            "studentAttendance" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)),
            "studentAttendanceRecords" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)),
            "studentPerformanceReport" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)),
            "classPerformance" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)),
            "teacherAnalytics" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(20))
        )
        
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)))
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}
