package com.opengnosis.scheduler.dto

import java.util.UUID

data class CreateClassroomRequest(
    val schoolId: UUID,
    val name: String,
    val capacity: Int
)

data class UpdateClassroomRequest(
    val name: String?,
    val capacity: Int?
)

data class ClassroomResponse(
    val id: UUID,
    val schoolId: UUID,
    val name: String,
    val capacity: Int
)

data class TeacherAvailabilityRequest(
    val teacherId: UUID,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String
)

data class TeacherAvailabilityResponse(
    val id: UUID,
    val teacherId: UUID,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String
)
