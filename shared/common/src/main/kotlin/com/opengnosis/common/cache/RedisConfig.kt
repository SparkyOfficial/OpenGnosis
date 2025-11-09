package com.opengnosis.common.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class RedisConfig {

    @Value("\${spring.redis.host:redis-service}")
    private lateinit var redisHost: String

    @Value("\${spring.redis.port:6379}")
    private var redisPort: Int = 6379

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val config = RedisStandaloneConfiguration(redisHost, redisPort)
        return LettuceConnectionFactory(config)
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        
        val objectMapper = ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
        
        val jsonSerializer = GenericJackson2JsonRedisSerializer(objectMapper)
        
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = jsonSerializer
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = jsonSerializer
        
        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val objectMapper = ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
        
        val jsonSerializer = GenericJackson2JsonRedisSerializer(objectMapper)
        
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
            )
            .disableCachingNullValues()

        val cacheConfigurations = mapOf(
            CacheNames.USER_CACHE to defaultConfig.entryTtl(Duration.ofMinutes(30)),
            CacheNames.STRUCTURE_CACHE to defaultConfig.entryTtl(Duration.ofMinutes(15)),
            CacheNames.SCHEDULE_CACHE to defaultConfig.entryTtl(Duration.ofMinutes(20)),
            CacheNames.ANALYTICS_CACHE to defaultConfig.entryTtl(Duration.ofMinutes(5)),
            CacheNames.SESSION_CACHE to defaultConfig.entryTtl(Duration.ofHours(2)),
            CacheNames.TOKEN_BLACKLIST to defaultConfig.entryTtl(Duration.ofHours(24))
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}
