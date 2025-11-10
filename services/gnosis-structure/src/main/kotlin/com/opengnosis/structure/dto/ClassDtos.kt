package com.opengnosis.structure.dto

import com.opengnosis.structure.domain.entity.ClassStatus
import jakarta.validation.constraints.*
import java.time.Instant
import java.util.UUID

data class CreateClassRequest(
    @field:NotNull(message = "School ID is required")
    val schoolId: UUID,
    
    @field:NotNull(message = "Academic year ID is required")
    val academicYearId: UUID,
    
    @field:NotBlank(message = "Class name is required")
    val name: String,
    
    @field:NotNull(message = "Grade is required")
    @field:Min(value = 1, message = "Grade must be at least 1")
    @field:Max(value = 12, message = "Grade must be at most 12")
    val grade: Int,
    
    @field:NotNull(message = "Class teacher ID is required")
    val classTeacherId: UUID,
    
    @field:Min(value = 1, message = "Capacity must be at least 1")
    val capacity: Int = 30
)

data class UpdateClassRequest(
    val name: String?,
    val grade: Int?,
    val classTeacherId: UUID?,
    val capacity: Int?,
    val status: ClassStatus?
)

data class ClassResponse(
    val id: UUID,
    val schoolId: UUID,
    val academicYearId: UUID,
    val name: String,
    val grade: Int,
    val classTeacherId: UUID,
    val capacity: Int,
    val status: ClassStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)
