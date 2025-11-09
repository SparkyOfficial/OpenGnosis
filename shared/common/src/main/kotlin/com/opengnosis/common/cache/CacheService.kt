package com.opengnosis.common.cache

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Service for interacting with Redis cache.
 * Provides common caching operations with TTL support.
 */
@Service
class CacheService(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(CacheService::class.java)

    /**
     * Store a value in cache with TTL.
     */
    fun set(key: String, value: Any, ttl: Duration) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl)
            logger.debug("Cached value for key: $key with TTL: ${ttl.seconds}s")
        } catch (ex: Exception) {
            logger.error("Failed to cache value for key: $key", ex)
        }
    }

    /**
     * Retrieve a value from cache.
     */
    fun <T> get(key: String, type: Class<T>): T? {
        return try {
            val value = redisTemplate.opsForValue().get(key)
            if (value != null && type.isInstance(value)) {
                logger.debug("Cache hit for key: $key")
                type.cast(value)
            } else {
                logger.debug("Cache miss for key: $key")
                null
            }
        } catch (ex: Exception) {
            logger.error("Failed to retrieve value for key: $key", ex)
            null
        }
    }

    /**
     * Delete a value from cache.
     */
    fun delete(key: String) {
        try {
            redisTemplate.delete(key)
            logger.debug("Deleted cache key: $key")
        } catch (ex: Exception) {
            logger.error("Failed to delete cache key: $key", ex)
        }
    }

    /**
     * Delete multiple keys matching a pattern.
     */
    fun deletePattern(pattern: String) {
        try {
            val keys = redisTemplate.keys(pattern)
            if (keys.isNotEmpty()) {
                redisTemplate.delete(keys)
                logger.debug("Deleted ${keys.size} cache keys matching pattern: $pattern")
            }
        } catch (ex: Exception) {
            logger.error("Failed to delete cache keys matching pattern: $pattern", ex)
        }
    }

    /**
     * Check if a key exists in cache.
     */
    fun exists(key: String): Boolean {
        return try {
            redisTemplate.hasKey(key)
        } catch (ex: Exception) {
            logger.error("Failed to check existence of key: $key", ex)
            false
        }
    }

    /**
     * Set expiration time for a key.
     */
    fun expire(key: String, ttl: Duration): Boolean {
        return try {
            redisTemplate.expire(key, ttl) ?: false
        } catch (ex: Exception) {
            logger.error("Failed to set expiration for key: $key", ex)
            false
        }
    }

    /**
     * Add value to a set.
     */
    fun addToSet(key: String, value: Any): Boolean {
        return try {
            redisTemplate.opsForSet().add(key, value) ?: 0 > 0
        } catch (ex: Exception) {
            logger.error("Failed to add value to set: $key", ex)
            false
        }
    }

    /**
     * Check if value exists in a set.
     */
    fun isMemberOfSet(key: String, value: Any): Boolean {
        return try {
            redisTemplate.opsForSet().isMember(key, value) ?: false
        } catch (ex: Exception) {
            logger.error("Failed to check set membership for key: $key", ex)
            false
        }
    }

    /**
     * Remove value from a set.
     */
    fun removeFromSet(key: String, value: Any): Boolean {
        return try {
            redisTemplate.opsForSet().remove(key, value) ?: 0 > 0
        } catch (ex: Exception) {
            logger.error("Failed to remove value from set: $key", ex)
            false
        }
    }
}
