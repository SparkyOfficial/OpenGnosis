package com.opengnosis.scheduler.dto

import com.opengnosis.scheduler.entity.ScheduleStatus
import java.util.UUID

data class CreateScheduleRequest(
    val academicYearId: UUID,
    val termId: UUID
)

data class ScheduleResponse(
    val id: UUID,
    val academicYearId: UUID,
    val termId: UUID,
    val status: ScheduleStatus,
    val entries: List<ScheduleEntryResponse> = emptyList()
)

data class CreateScheduleEntryRequest(
    val classId: UUID,
    val subjectId: UUID,
    val teacherId: UUID,
    val classroomId: UUID,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String
)

data class ScheduleEntryResponse(
    val id: UUID,
    val scheduleId: UUID,
    val classId: UUID,
    val subjectId: UUID,
    val teacherId: UUID,
    val classroomId: UUID,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String
)

data class ConflictInfo(
    val type: ConflictType,
    val message: String,
    val conflictingEntries: List<ScheduleEntryResponse>
)

enum class ConflictType {
    TEACHER_CONFLICT,
    CLASSROOM_CONFLICT,
    CLASS_CONFLICT,
    TEACHER_UNAVAILABLE
}

data class ValidationResult(
    val valid: Boolean,
    val conflicts: List<ConflictInfo> = emptyList()
)
