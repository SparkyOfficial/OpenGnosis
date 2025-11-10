package com.opengnosis.iam.dto

import com.opengnosis.domain.Role
import jakarta.validation.constraints.NotEmpty

data class AssignRolesRequest(
    @field:NotEmpty(message = "Roles cannot be empty")
    val roles: Set<Role>
)

data class RoleResponse(
    val userId: String,
    val roles: Set<Role>
)
