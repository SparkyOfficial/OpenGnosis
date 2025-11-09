package com.opengnosis.events

import java.time.Instant
import java.util.UUID

interface DomainEvent {
    val eventId: UUID
    val timestamp: Instant
    val eventType: String
    val aggregateId: UUID
    val version: Int
}

abstract class BaseDomainEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val version: Int = 1
) : DomainEvent
