package com.opengnosis.analytics.consumer

import com.opengnosis.analytics.service.AttendanceReadModelService
import com.opengnosis.events.AttendanceMarkedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class AttendanceMarkedEventConsumer(
    private val attendanceReadModelService: AttendanceReadModelService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @KafkaListener(
        topics = ["attendance-marked"],
        groupId = "analytics-query-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(event: AttendanceMarkedEvent, acknowledgment: Acknowledgment) {
        try {
            logger.info("Received AttendanceMarkedEvent: eventId=${event.eventId}, studentId=${event.studentId}, classId=${event.classId}")
            
            attendanceReadModelService.handleAttendanceMarkedEvent(event)
            
            acknowledgment.acknowledge()
            logger.info("Successfully processed AttendanceMarkedEvent: eventId=${event.eventId}")
        } catch (e: Exception) {
            logger.error("Error processing AttendanceMarkedEvent: eventId=${event.eventId}", e)
            throw e
        }
    }
}
