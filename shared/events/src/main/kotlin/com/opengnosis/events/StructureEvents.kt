package com.opengnosis.events

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class SchoolCreatedEvent(
    override val aggregateId: UUID,
    val name: String,
    val address: String,
    val principalId: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "SchoolCreated"
}

data class ClassCreatedEvent(
    override val aggregateId: UUID,
    val schoolId: UUID,
    val academicYearId: UUID,
    val name: String,
    val grade: Int,
    val classTeacherId: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "ClassCreated"
}

data class StudentEnrolledEvent(
    override val aggregateId: UUID,
    val studentId: UUID,
    val classId: UUID,
    val enrollmentDate: LocalDate,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "StudentEnrolled"
}

data class StudentUnenrolledEvent(
    override val aggregateId: UUID,
    val studentId: UUID,
    val classId: UUID,
    val reason: String,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "StudentUnenrolled"
}

data class TeacherAssignedEvent(
    override val aggregateId: UUID,
    val teacherId: UUID,
    val classId: UUID,
    val subjectId: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "TeacherAssigned"
}
