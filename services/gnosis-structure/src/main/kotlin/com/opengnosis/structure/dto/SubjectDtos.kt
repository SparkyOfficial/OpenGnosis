package com.opengnosis.structure.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class CreateSubjectRequest(
    @field:NotBlank(message = "Subject name is required")
    val name: String,
    
    @field:NotBlank(message = "Subject code is required")
    val code: String,
    
    val description: String? = null
)

data class UpdateSubjectRequest(
    val name: String?,
    val description: String?
)

data class SubjectResponse(
    val id: UUID,
    val name: String,
    val code: String,
    val description: String?,
    val createdAt: Instant
)

data class AssignSubjectToClassRequest(
    @field:NotNull(message = "Class ID is required")
    val classId: UUID,
    
    @field:NotNull(message = "Subject ID is required")
    val subjectId: UUID,
    
    @field:NotNull(message = "Teacher ID is required")
    val teacherId: UUID,
    
    val hoursPerWeek: Int = 1
)

data class ClassSubjectResponse(
    val id: UUID,
    val classId: UUID,
    val subjectId: UUID,
    val subjectName: String,
    val subjectCode: String,
    val teacherId: UUID,
    val hoursPerWeek: Int,
    val createdAt: Instant
)
