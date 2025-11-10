package com.opengnosis.structure.dto

import com.opengnosis.domain.SchoolStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class CreateSchoolRequest(
    @field:NotBlank(message = "School name is required")
    val name: String,
    
    @field:NotBlank(message = "Address is required")
    val address: String,
    
    @field:NotNull(message = "Principal ID is required")
    val principalId: UUID
)

data class UpdateSchoolRequest(
    val name: String?,
    val address: String?,
    val principalId: UUID?,
    val status: SchoolStatus?
)

data class SchoolResponse(
    val id: UUID,
    val name: String,
    val address: String,
    val principalId: UUID,
    val status: SchoolStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)
