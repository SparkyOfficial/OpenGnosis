package com.opengnosis.common.kafka

import com.opengnosis.events.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(EventPublisher::class.java)

    fun publish(event: DomainEvent) {
        val topic = getTopicForEvent(event.eventType)
        logger.info("Publishing event ${event.eventType} with ID ${event.eventId} to topic $topic")
        
        kafkaTemplate.send(topic, event.aggregateId.toString(), event)
            .whenComplete { result, ex ->
                if (ex != null) {
                    logger.error("Failed to publish event ${event.eventId}", ex)
                } else {
                    logger.debug("Event ${event.eventId} published successfully to partition ${result?.recordMetadata?.partition()}")
                }
            }
    }

    private fun getTopicForEvent(eventType: String): String {
        return when {
            eventType.contains("Grade") || eventType.contains("Attendance") || eventType.contains("Homework") -> "journal-events"
            eventType.contains("User") -> "user-events"
            eventType.contains("School") || eventType.contains("Class") || eventType.contains("Student") || eventType.contains("Teacher") -> "structure-events"
            eventType.contains("Schedule") -> "schedule-events"
            else -> "domain-events"
        }
    }
}
