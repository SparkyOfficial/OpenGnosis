package com.opengnosis.common.cache

/**
 * Cache name constants for the OpenGnosis platform.
 * Each cache has specific TTL policies configured in RedisConfig.
 */
object CacheNames {
    const val USER_CACHE = "users"
    const val STRUCTURE_CACHE = "structure"
    const val SCHEDULE_CACHE = "schedules"
    const val ANALYTICS_CACHE = "analytics"
    const val SESSION_CACHE = "sessions"
    const val TOKEN_BLACKLIST = "token-blacklist"
}
