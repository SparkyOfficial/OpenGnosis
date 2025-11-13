package com.opengnosis.analytics.consumer

import com.opengnosis.analytics.service.EnrollmentReadModelService
import com.opengnosis.events.StudentEnrolledEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class StudentEnrolledEventConsumer(
    private val enrollmentReadModelService: EnrollmentReadModelService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @KafkaListener(
        topics = ["student-enrolled"],
        groupId = "analytics-query-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(event: StudentEnrolledEvent, acknowledgment: Acknowledgment) {
        try {
            logger.info("Received StudentEnrolledEvent: eventId=${event.eventId}, studentId=${event.studentId}, classId=${event.classId}")
            
            enrollmentReadModelService.handleStudentEnrolledEvent(event)
            
            acknowledgment.acknowledge()
            logger.info("Successfully processed StudentEnrolledEvent: eventId=${event.eventId}")
        } catch (e: Exception) {
            logger.error("Error processing StudentEnrolledEvent: eventId=${event.eventId}", e)
            throw e
        }
    }
}
