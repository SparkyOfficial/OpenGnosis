package com.opengnosis.notifier.consumer

import com.opengnosis.domain.NotificationType
import com.opengnosis.events.ScheduleModifiedEvent
import com.opengnosis.notifier.service.NotificationDispatchService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ScheduleModifiedEventConsumer(
    private val notificationDispatchService: NotificationDispatchService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @KafkaListener(
        topics = ["schedule-events"],
        groupId = "gnosis-notifier-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeScheduleModifiedEvent(event: ScheduleModifiedEvent) {
        logger.info("Received ScheduleModifiedEvent for schedule: ${event.scheduleId}")
        
        try {
            // In a real system, we would fetch all affected users (students, teachers, parents)
            // from the Structure service and send notifications to each
            val title = "Schedule Change"
            val content = """
                Your class schedule has been modified.
                
                Change: ${event.changeDescription}
                Affected Classes: ${event.affectedClasses.size}
                Affected Teachers: ${event.affectedTeachers.size}
                
                Please check your updated schedule.
            """.trimIndent()
            
            // Send to all affected teachers
            event.affectedTeachers.forEach { teacherId ->
                notificationDispatchService.sendNotification(
                    userId = teacherId,
                    type = NotificationType.SCHEDULE_CHANGE,
                    title = title,
                    content = content,
                    email = null, // Would be fetched from user service
                    phoneNumber = null
                )
            }
            
            // TODO: Fetch students from affected classes and send to each
            logger.info("Would send schedule change notification to all students in classes: ${event.affectedClasses}")
            
            logger.info("Successfully processed ScheduleModifiedEvent for schedule: ${event.scheduleId}")
        } catch (e: Exception) {
            logger.error("Failed to process ScheduleModifiedEvent for schedule: ${event.scheduleId}", e)
        }
    }
}
