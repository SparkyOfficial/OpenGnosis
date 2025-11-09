package com.opengnosis.domain

import java.time.Instant
import java.util.UUID

data class StudentGradesReadModel(
    val studentId: UUID,
    val subjectId: UUID,
    val grades: List<GradeEntry>,
    val average: Double,
    val lastUpdated: Instant
)

data class GradeEntry(
    val id: UUID,
    val value: Int,
    val type: GradeType,
    val comment: String?,
    val createdAt: Instant
)

data class StudentAttendanceReadModel(
    val studentId: UUID,
    val classId: UUID,
    val totalLessons: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val attendanceRate: Double
)

data class ClassPerformanceReadModel(
    val classId: UUID,
    val subjectId: UUID,
    val averageGrade: Double,
    val studentCount: Int,
    val gradeDistribution: Map<Int, Int>
)
