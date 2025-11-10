package com.opengnosis.iam.service

import com.opengnosis.common.kafka.EventPublisher
import com.opengnosis.domain.Role
import com.opengnosis.events.UserRoleChangedEvent
import com.opengnosis.iam.domain.entity.UserEntity
import com.opengnosis.iam.repository.RoleRepository
import com.opengnosis.iam.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RoleService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val eventPublisher: EventPublisher
) {
    
    @Transactional
    fun assignRoles(userId: UUID, roles: Set<Role>, changedBy: UUID): UserEntity {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        val oldRoles = user.getRoleNames()
        
        // Get role entities
        val roleEntities = roleRepository.findByNameIn(roles.map { it.name }.toSet())
        if (roleEntities.isEmpty()) {
            throw IllegalArgumentException("No valid roles provided")
        }
        
        // Update user roles
        user.roles.clear()
        user.roles.addAll(roleEntities)
        
        val updatedUser = userRepository.save(user)
        
        // Publish event
        val event = UserRoleChangedEvent(
            aggregateId = userId,
            oldRoles = oldRoles,
            newRoles = updatedUser.getRoleNames(),
            changedBy = changedBy
        )
        eventPublisher.publish("user-events", event)
        
        return updatedUser
    }
    
    @Transactional
    fun addRole(userId: UUID, role: Role, changedBy: UUID): UserEntity {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        val oldRoles = user.getRoleNames()
        
        val roleEntity = roleRepository.findByName(role.name)
            ?: throw IllegalArgumentException("Role not found: ${role.name}")
        
        user.roles.add(roleEntity)
        val updatedUser = userRepository.save(user)
        
        // Publish event
        val event = UserRoleChangedEvent(
            aggregateId = userId,
            oldRoles = oldRoles,
            newRoles = updatedUser.getRoleNames(),
            changedBy = changedBy
        )
        eventPublisher.publish("user-events", event)
        
        return updatedUser
    }
    
    @Transactional
    fun removeRole(userId: UUID, role: Role, changedBy: UUID): UserEntity {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        val oldRoles = user.getRoleNames()
        
        val roleEntity = roleRepository.findByName(role.name)
            ?: throw IllegalArgumentException("Role not found: ${role.name}")
        
        user.roles.remove(roleEntity)
        
        if (user.roles.isEmpty()) {
            throw IllegalStateException("User must have at least one role")
        }
        
        val updatedUser = userRepository.save(user)
        
        // Publish event
        val event = UserRoleChangedEvent(
            aggregateId = userId,
            oldRoles = oldRoles,
            newRoles = updatedUser.getRoleNames(),
            changedBy = changedBy
        )
        eventPublisher.publish("user-events", event)
        
        return updatedUser
    }
    
    fun getUserRoles(userId: UUID): Set<Role> {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }
        
        return user.getRoleNames()
    }
}
