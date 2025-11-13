package com.opengnosis.analytics.consumer

import com.opengnosis.analytics.service.GradesReadModelService
import com.opengnosis.events.GradePlacedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class GradePlacedEventConsumer(
    private val gradesReadModelService: GradesReadModelService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @KafkaListener(
        topics = ["grade-placed"],
        groupId = "analytics-query-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(event: GradePlacedEvent, acknowledgment: Acknowledgment) {
        try {
            logger.info("Received GradePlacedEvent: eventId=${event.eventId}, studentId=${event.studentId}, subjectId=${event.subjectId}")
            
            gradesReadModelService.handleGradePlacedEvent(event)
            
            acknowledgment.acknowledge()
            logger.info("Successfully processed GradePlacedEvent: eventId=${event.eventId}")
        } catch (e: Exception) {
            logger.error("Error processing GradePlacedEvent: eventId=${event.eventId}", e)
            throw e
        }
    }
}
