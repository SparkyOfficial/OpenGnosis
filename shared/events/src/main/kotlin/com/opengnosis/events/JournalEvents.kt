package com.opengnosis.events

import com.opengnosis.domain.AttendanceStatus
import com.opengnosis.domain.GradeType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class GradePlacedEvent(
    override val aggregateId: UUID,
    val studentId: UUID,
    val subjectId: UUID,
    val gradeValue: Int,
    val gradeType: GradeType,
    val comment: String?,
    val placedBy: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "GradePlaced"
}

data class AttendanceMarkedEvent(
    override val aggregateId: UUID,
    val studentId: UUID,
    val classId: UUID,
    val date: LocalDate,
    val lessonNumber: Int,
    val status: AttendanceStatus,
    val markedBy: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "AttendanceMarked"
}

data class HomeworkAssignedEvent(
    override val aggregateId: UUID,
    val classId: UUID,
    val subjectId: UUID,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val assignedBy: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "HomeworkAssigned"
}
