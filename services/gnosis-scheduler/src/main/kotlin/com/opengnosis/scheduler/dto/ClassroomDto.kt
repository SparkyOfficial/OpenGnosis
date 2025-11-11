package com.opengnosis.scheduler.dto

import com.opengnosis.scheduler.entity.Equipment
import java.util.UUID

data class CreateClassroomRequest(
    val schoolId: UUID,
    val name: String,
    val capacity: Int,
    val equipment: Set<Equipment> = emptySet()
)

data class UpdateClassroomRequest(
    val name: String?,
    val capacity: Int?,
    val equipment: Set<Equipment>?
)

data class ClassroomResponse(
    val id: UUID,
    val schoolId: UUID,
    val name: String,
    val capacity: Int,
    val equipment: Set<Equipment>
)

data class TeacherAvailabilityRequest(
    val teacherId: UUID,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val available: Boolean = true
)

data class TeacherAvailabilityResponse(
    val id: UUID,
    val teacherId: UUID,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val available: Boolean
)
