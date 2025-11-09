package com.opengnosis.domain

import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

data class Schedule(
    val id: UUID,
    val academicYearId: UUID,
    val termId: UUID,
    val entries: List<ScheduleEntry>,
    val status: ScheduleStatus
)

enum class ScheduleStatus {
    DRAFT, PUBLISHED, ARCHIVED
}

data class ScheduleEntry(
    val id: UUID,
    val scheduleId: UUID,
    val classId: UUID,
    val subjectId: UUID,
    val teacherId: UUID,
    val classroomId: UUID,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime
)

data class Classroom(
    val id: UUID,
    val schoolId: UUID,
    val name: String,
    val capacity: Int,
    val equipment: Set<Equipment>
)

enum class Equipment {
    PROJECTOR, WHITEBOARD, COMPUTERS, LAB_EQUIPMENT, SPORTS_EQUIPMENT
}
