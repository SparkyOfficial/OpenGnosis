package com.opengnosis.domain

import java.time.LocalDate
import java.util.UUID

data class School(
    val id: UUID,
    val name: String,
    val address: String,
    val principalId: UUID,
    val status: SchoolStatus
)

enum class SchoolStatus {
    ACTIVE, INACTIVE, ARCHIVED
}

data class AcademicYear(
    val id: UUID,
    val schoolId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val terms: List<Term>
)

data class Term(
    val id: UUID,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class Class(
    val id: UUID,
    val schoolId: UUID,
    val academicYearId: UUID,
    val name: String,
    val grade: Int,
    val classTeacherId: UUID
)

data class Subject(
    val id: UUID,
    val name: String,
    val code: String,
    val description: String
)

data class Enrollment(
    val id: UUID,
    val studentId: UUID,
    val classId: UUID,
    val enrollmentDate: LocalDate,
    val status: EnrollmentStatus
)

enum class EnrollmentStatus {
    ACTIVE, COMPLETED, WITHDRAWN
}
