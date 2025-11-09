package com.opengnosis.domain

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val firstName: String,
    val lastName: String,
    val roles: Set<Role>,
    val status: UserStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class Role {
    STUDENT, TEACHER, PARENT, ADMINISTRATOR, SYSTEM_ADMIN
}

enum class UserStatus {
    ACTIVE, SUSPENDED, DELETED
}
