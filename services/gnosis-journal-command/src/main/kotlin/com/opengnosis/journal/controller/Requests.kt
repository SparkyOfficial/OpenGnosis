package com.opengnosis.journal.controller

import com.opengnosis.domain.AttendanceStatus
import com.opengnosis.domain.GradeType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class PlaceGradeRequest(
    val commandId: UUID? = null,
    val issuedBy: UUID,
    val studentId: UUID,
    val subjectId: UUID,
    val gradeValue: Int,
    val gradeType: GradeType,
    val comment: String? = null
)

data class MarkAttendanceRequest(
    val commandId: UUID? = null,
    val issuedBy: UUID,
    val studentId: UUID,
    val classId: UUID,
    val date: LocalDate,
    val lessonNumber: Int,
    val status: AttendanceStatus
)

data class AssignHomeworkRequest(
    val commandId: UUID? = null,
    val issuedBy: UUID,
    val classId: UUID,
    val subjectId: UUID,
    val title: String,
    val description: String,
    val dueDate: LocalDate
)

data class CommandStatusResponse(
    val commandId: UUID,
    val commandType: String,
    val status: String,
    val issuedBy: UUID,
    val timestamp: Instant,
    val processedAt: Instant,
    val errorMessage: String?
)
