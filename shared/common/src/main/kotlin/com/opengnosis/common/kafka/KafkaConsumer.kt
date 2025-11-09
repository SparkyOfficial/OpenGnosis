package com.opengnosis.common.kafka

import com.opengnosis.events.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.Acknowledgment

/**
 * Base interface for Kafka event consumers.
 * Implementations should handle specific event types.
 */
interface EventConsumer<T : DomainEvent> {
    fun consume(event: T, acknowledgment: Acknowledgment)
}

/**
 * Abstract base class for Kafka event consumers with error handling.
 */
abstract class BaseEventConsumer<T : DomainEvent> : EventConsumer<T> {
    protected val logger = LoggerFactory.getLogger(this::class.java)
    
    override fun consume(event: T, acknowledgment: Acknowledgment) {
        try {
            logger.info("Consuming event ${event.eventType} with ID ${event.eventId}")
            processEvent(event)
            acknowledgment.acknowledge()
            logger.debug("Event ${event.eventId} processed successfully")
        } catch (ex: Exception) {
            logger.error("Failed to process event ${event.eventId}", ex)
            // Don't acknowledge - message will be redelivered
            throw ex
        }
    }
    
    /**
     * Process the event. Implementations should override this method.
     */
    protected abstract fun processEvent(event: T)
}
