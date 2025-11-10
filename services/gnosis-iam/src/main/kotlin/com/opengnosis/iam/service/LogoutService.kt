package com.opengnosis.iam.service

import com.opengnosis.iam.repository.RefreshTokenRepository
import com.opengnosis.iam.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LogoutService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val jwtTokenValidator: JwtTokenValidator
) {
    
    @Value("\${jwt.expiration:86400000}") // 24 hours
    private var jwtExpiration: Long = 86400000
    
    @Transactional
    fun logout(accessToken: String, refreshToken: String?) {
        // Blacklist the access token
        jwtTokenValidator.blacklistToken(accessToken, jwtExpiration / 1000)
        
        // Revoke refresh token if provided
        refreshToken?.let {
            val tokenEntity = refreshTokenRepository.findByToken(it)
            tokenEntity?.let { token ->
                token.revoked = true
                refreshTokenRepository.save(token)
            }
        }
    }
    
    @Transactional
    fun logoutAllSessions(userId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        // Revoke all refresh tokens for the user
        refreshTokenRepository.revokeAllByUser(user)
    }
}
