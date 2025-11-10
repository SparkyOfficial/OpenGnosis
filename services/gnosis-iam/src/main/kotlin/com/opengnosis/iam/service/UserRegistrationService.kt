package com.opengnosis.iam.service

import com.opengnosis.common.kafka.EventPublisher
import com.opengnosis.domain.Role
import com.opengnosis.domain.UserStatus
import com.opengnosis.events.UserRegisteredEvent
import com.opengnosis.iam.domain.entity.UserEntity
import com.opengnosis.iam.dto.RegisterRequest
import com.opengnosis.iam.repository.RoleRepository
import com.opengnosis.iam.repository.UserRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val eventPublisher: EventPublisher
) {
    
    @Transactional
    fun registerUser(request: RegisterRequest): UserEntity {
        // Validate email uniqueness
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }
        
        // Validate email format
        if (!isValidEmail(request.email)) {
            throw IllegalArgumentException("Invalid email format")
        }
        
        // Hash password
        val passwordHash = passwordEncoder.encode(request.password)
        
        // Create user entity
        val user = UserEntity(
            email = request.email,
            passwordHash = passwordHash,
            firstName = request.firstName,
            lastName = request.lastName,
            status = UserStatus.ACTIVE
        )
        
        // Assign roles
        val roleEntities = roleRepository.findByNameIn(request.roles.map { it.name }.toSet())
        if (roleEntities.isEmpty()) {
            throw IllegalStateException("No valid roles found")
        }
        user.roles.addAll(roleEntities)
        
        // Save user
        val savedUser = userRepository.save(user)
        
        // Publish event
        val event = UserRegisteredEvent(
            aggregateId = savedUser.id,
            email = savedUser.email,
            firstName = savedUser.firstName,
            lastName = savedUser.lastName,
            roles = savedUser.getRoleNames()
        )
        eventPublisher.publish("user-events", event)
        
        return savedUser
    }
    
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
}
