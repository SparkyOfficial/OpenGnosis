package com.opengnosis.iam.service

import com.opengnosis.common.kafka.EventPublisher
import com.opengnosis.common.security.JwtTokenProvider
import com.opengnosis.events.UserAuthenticatedEvent
import com.opengnosis.iam.domain.entity.RefreshTokenEntity
import com.opengnosis.iam.domain.entity.UserEntity
import com.opengnosis.iam.dto.AuthResponse
import com.opengnosis.iam.dto.LoginRequest
import com.opengnosis.iam.repository.RefreshTokenRepository
import com.opengnosis.iam.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val eventPublisher: EventPublisher
) {
    
    @Value("\${jwt.expiration:86400000}") // 24 hours
    private var jwtExpiration: Long = 86400000
    
    @Value("\${jwt.refresh-expiration:604800000}") // 7 days
    private var refreshExpiration: Long = 604800000
    
    @Transactional
    fun authenticate(request: LoginRequest, ipAddress: String = "unknown"): AuthResponse {
        // Find user by email
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Invalid credentials")
        
        // Validate password
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }
        
        // Check user status
        if (user.status != com.opengnosis.domain.UserStatus.ACTIVE) {
            throw IllegalStateException("User account is not active")
        }
        
        // Generate tokens
        val accessToken = jwtTokenProvider.generateToken(
            userId = user.id.toString(),
            email = user.email,
            roles = user.getRoleNames().map { it.name }.toSet()
        )
        
        val refreshToken = generateRefreshToken(user)
        
        // Publish authentication event
        val event = UserAuthenticatedEvent(
            aggregateId = user.id,
            email = user.email,
            ipAddress = ipAddress
        )
        eventPublisher.publish("user-events", event)
        
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            expiresIn = jwtExpiration / 1000, // Convert to seconds
            userId = user.id.toString(),
            email = user.email,
            roles = user.getRoleNames()
        )
    }
    
    private fun generateRefreshToken(user: UserEntity): RefreshTokenEntity {
        val token = UUID.randomUUID().toString()
        val expiresAt = Instant.now().plusMillis(refreshExpiration)
        
        val refreshToken = RefreshTokenEntity(
            user = user,
            token = token,
            expiresAt = expiresAt
        )
        
        return refreshTokenRepository.save(refreshToken)
    }
}
