package com.opengnosis.events

import com.opengnosis.domain.Role
import java.time.Instant
import java.util.UUID

data class UserRegisteredEvent(
    override val aggregateId: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val roles: Set<Role>,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "UserRegistered"
}

data class UserAuthenticatedEvent(
    override val aggregateId: UUID,
    val email: String,
    val ipAddress: String,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "UserAuthenticated"
}

data class UserRoleChangedEvent(
    override val aggregateId: UUID,
    val oldRoles: Set<Role>,
    val newRoles: Set<Role>,
    val changedBy: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "UserRoleChanged"
}

data class UserSuspendedEvent(
    override val aggregateId: UUID,
    val reason: String,
    val suspendedBy: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : BaseDomainEvent(eventId, timestamp, version) {
    override val eventType: String = "UserSuspended"
}
