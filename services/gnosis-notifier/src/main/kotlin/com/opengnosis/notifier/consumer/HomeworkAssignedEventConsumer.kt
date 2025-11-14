package com.opengnosis.notifier.consumer

import com.opengnosis.domain.NotificationType
import com.opengnosis.events.HomeworkAssignedEvent
import com.opengnosis.notifier.service.NotificationDispatchService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class HomeworkAssignedEventConsumer(
    private val notificationDispatchService: NotificationDispatchService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @KafkaListener(
        topics = ["journal-events"],
        groupId = "gnosis-notifier-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeHomeworkAssignedEvent(event: HomeworkAssignedEvent) {
        logger.info("Received HomeworkAssignedEvent for class: ${event.classId}")
        
        try {
            // In a real system, we would fetch all students in the class from Structure service
            // and send notifications to each student
            val title = "New Homework Assignment"
            val content = """
                New homework has been assigned to your class.
                
                Title: ${event.title}
                Description: ${event.description}
                Due Date: ${event.dueDate}
                
                Please complete the assignment before the due date.
            """.trimIndent()
            
            // TODO: Fetch students from class and send to each
            // For now, we'll just log that we would send notifications
            logger.info("Would send homework notification to all students in class: ${event.classId}")
            
            // Example of how it would work for a single student:
            // notificationDispatchService.sendNotification(
            //     userId = studentId,
            //     type = NotificationType.HOMEWORK_ASSIGNED,
            //     title = title,
            //     content = content,
            //     email = studentEmail,
            //     phoneNumber = null
            // )
            
            logger.info("Successfully processed HomeworkAssignedEvent for class: ${event.classId}")
        } catch (e: Exception) {
            logger.error("Failed to process HomeworkAssignedEvent for class: ${event.classId}", e)
        }
    }
}
