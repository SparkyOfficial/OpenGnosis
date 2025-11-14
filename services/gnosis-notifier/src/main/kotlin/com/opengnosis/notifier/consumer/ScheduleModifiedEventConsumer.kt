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
        logger.info("Received ScheduleModifiedEvent for class: ${event.classId}")
        
        try {
            // In a real system, we would fetch all affected users (students, teachers, parents)
            // from the Structure service and send notifications to each
            val title = "Schedule Change"
            val content = """
                Your class schedule has been modified.
                
                Day: ${event.dayOfWeek}
                Time: ${event.startTime} - ${event.endTime}
                Classroom: ${event.classroomId}
                
                Please check your updated schedule.
            """.trimIndent()
            
            // Send to teacher
            notificationDispatchService.sendNotification(
                userId = event.teacherId,
                type = NotificationType.SCHEDULE_CHANGE,
                title = title,
                content = content,
                email = null, // Would be fetched from user service
                phoneNumber = null
            )
            
            // TODO: Fetch students from class and send to each
            logger.info("Would send schedule change notification to all students in class: ${event.classId}")
            
            logger.info("Successfully processed ScheduleModifiedEvent for class: ${event.classId}")
        } catch (e: Exception) {
            logger.error("Failed to process ScheduleModifiedEvent for class: ${event.classId}", e)
        }
    }
}
