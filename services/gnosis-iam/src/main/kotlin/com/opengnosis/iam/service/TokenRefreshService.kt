package com.opengnosis.iam.service

import com.opengnosis.common.security.JwtTokenProvider
import com.opengnosis.iam.domain.entity.RefreshTokenEntity
import com.opengnosis.iam.dto.AuthResponse
import com.opengnosis.iam.repository.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class TokenRefreshService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {
    
    @Value("\${jwt.expiration:86400000}") // 24 hours
    private var jwtExpiration: Long = 86400000
    
    @Value("\${jwt.refresh-expiration:604800000}") // 7 days
    private var refreshExpiration: Long = 604800000
    
    @Transactional
    fun refreshToken(refreshToken: String): AuthResponse {
        // Find refresh token
        val tokenEntity = refreshTokenRepository.findByToken(refreshToken)
            ?: throw IllegalArgumentException("Invalid refresh token")
        
        // Check if token is revoked
        if (tokenEntity.revoked) {
            throw IllegalArgumentException("Refresh token has been revoked")
        }
        
        // Check if token is expired
        if (tokenEntity.expiresAt.isBefore(Instant.now())) {
            throw IllegalArgumentException("Refresh token has expired")
        }
        
        val user = tokenEntity.user
        
        // Check user status
        if (user.status != com.opengnosis.domain.UserStatus.ACTIVE) {
            throw IllegalStateException("User account is not active")
        }
        
        // Generate new access token
        val accessToken = jwtTokenProvider.generateToken(
            userId = user.id.toString(),
            email = user.email,
            roles = user.getRoleNames().map { it.name }.toSet()
        )
        
        // Rotate refresh token (revoke old, create new)
        tokenEntity.revoked = true
        refreshTokenRepository.save(tokenEntity)
        
        val newRefreshToken = RefreshTokenEntity(
            user = user,
            token = UUID.randomUUID().toString(),
            expiresAt = Instant.now().plusMillis(refreshExpiration)
        )
        refreshTokenRepository.save(newRefreshToken)
        
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = newRefreshToken.token,
            expiresIn = jwtExpiration / 1000,
            userId = user.id.toString(),
            email = user.email,
            roles = user.getRoleNames()
        )
    }
    
    @Transactional
    fun cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now())
    }
}
