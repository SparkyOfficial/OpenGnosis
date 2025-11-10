package com.opengnosis.iam.repository

import com.opengnosis.domain.UserStatus
import com.opengnosis.iam.domain.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun existsByEmail(email: String): Boolean
    fun findByStatus(status: UserStatus): List<UserEntity>
}
