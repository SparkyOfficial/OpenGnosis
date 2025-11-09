package com.opengnosis.events

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

data class ScheduleCreatedEvent(
    override val aggregateId: UUID,
    val academicYearId: UUID,
    val termId: UUID,
    val createdBy: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "ScheduleCreated"
}

data class ScheduleModifiedEvent(
    override val aggregateId: UUID,
    val scheduleEntryId: UUID,
    val classId: UUID,
    val subjectId: UUID,
    val teacherId: UUID,
    val classroomId: UUID,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val modifiedBy: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "ScheduleModified"
}

data class ScheduleConflictDetectedEvent(
    override val aggregateId: UUID,
    val conflictType: String,
    val conflictingEntries: List<UUID>,
    val details: String,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "ScheduleConflictDetected"
}
