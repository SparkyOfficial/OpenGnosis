package com.opengnosis.notifier.consumer

import com.opengnosis.domain.NotificationType
import com.opengnosis.events.GradePlacedEvent
import com.opengnosis.notifier.service.NotificationDispatchService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class GradePlacedEventConsumer(
    private val notificationDispatchService: NotificationDispatchService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @KafkaListener(
        topics = ["journal-events"],
        groupId = "gnosis-notifier-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeGradePlacedEvent(event: GradePlacedEvent) {
        logger.info("Received GradePlacedEvent for student: ${event.studentId}")
        
        try {
            // In a real system, we would fetch parent/student contact info from IAM service
            // For now, we'll use the student ID as the recipient
            val title = "New Grade Posted"
            val content = """
                A new grade has been posted for your student.
                
                Grade: ${event.gradeValue}
                Type: ${event.gradeType}
                ${event.comment?.let { "Comment: $it" } ?: ""}
                
                Please log in to view more details.
            """.trimIndent()
            
            // Send notification to student
            notificationDispatchService.sendNotification(
                userId = event.studentId,
                type = NotificationType.NEW_GRADE,
                title = title,
                content = content,
                email = null, // Would be fetched from user service
                phoneNumber = null
            )
            
            logger.info("Successfully processed GradePlacedEvent for student: ${event.studentId}")
        } catch (e: Exception) {
            logger.error("Failed to process GradePlacedEvent for student: ${event.studentId}", e)
        }
    }
}
