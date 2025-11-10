package com.opengnosis.structure.dto

import com.opengnosis.domain.EnrollmentStatus
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class EnrollStudentRequest(
    @field:NotNull(message = "Student ID is required")
    val studentId: UUID,
    
    @field:NotNull(message = "Class ID is required")
    val classId: UUID,
    
    val enrollmentDate: LocalDate = LocalDate.now()
)

data class UnenrollStudentRequest(
    val reason: String,
    val unenrollmentDate: LocalDate = LocalDate.now()
)

data class EnrollmentResponse(
    val id: UUID,
    val studentId: UUID,
    val classId: UUID,
    val enrollmentDate: LocalDate,
    val unenrollmentDate: LocalDate?,
    val status: EnrollmentStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ClassCompositionResponse(
    val classId: UUID,
    val className: String,
    val totalStudents: Int,
    val capacity: Int,
    val students: List<StudentEnrollmentInfo>
)

data class StudentEnrollmentInfo(
    val enrollmentId: UUID,
    val studentId: UUID,
    val enrollmentDate: LocalDate,
    val status: EnrollmentStatus
)
