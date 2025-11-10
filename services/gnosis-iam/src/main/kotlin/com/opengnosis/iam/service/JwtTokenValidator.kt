package com.opengnosis.iam.service

import com.opengnosis.common.security.JwtTokenProvider
import com.opengnosis.iam.repository.UserRepository
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class JwtTokenValidator(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val redisTemplate: RedisTemplate<String, String>
) {
    
    companion object {
        private const val TOKEN_BLACKLIST_PREFIX = "blacklist:token:"
    }
    
    fun validateToken(token: String): Boolean {
        // Check if token is blacklisted
        if (isTokenBlacklisted(token)) {
            return false
        }
        
        // Validate token signature and expiration
        if (!jwtTokenProvider.validateToken(token)) {
            return false
        }
        
        // Verify user still exists and is active
        val userId = jwtTokenProvider.getUserIdFromToken(token)
        val user = userRepository.findById(java.util.UUID.fromString(userId))
        
        return user.isPresent && user.get().status == com.opengnosis.domain.UserStatus.ACTIVE
    }
    
    fun blacklistToken(token: String, expirationSeconds: Long) {
        val key = TOKEN_BLACKLIST_PREFIX + token
        redisTemplate.opsForValue().set(key, "true", expirationSeconds, TimeUnit.SECONDS)
    }
    
    private fun isTokenBlacklisted(token: String): Boolean {
        val key = TOKEN_BLACKLIST_PREFIX + token
        return redisTemplate.hasKey(key) == true
    }
}
