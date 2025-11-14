package com.opengnosis.notifier.service

import com.opengnosis.domain.DeliveryStatus
import com.opengnosis.domain.NotificationChannel
import com.opengnosis.domain.NotificationType
import com.opengnosis.notifier.config.NotificationConfig
import com.opengnosis.notifier.entity.NotificationDeliveryEntity
import com.opengnosis.notifier.repository.NotificationDeliveryRepository
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class EmailNotificationService(
    private val mailSender: JavaMailSender,
    private val notificationConfig: NotificationConfig,
    private val deliveryRepository: NotificationDeliveryRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @Async
    fun sendEmail(
        userId: UUID,
        recipientEmail: String,
        type: NotificationType,
        subject: String,
        content: String
    ) {
        if (!notificationConfig.email.enabled) {
            logger.info("Email notifications are disabled")
            return
        }
        
        val delivery = NotificationDeliveryEntity(
            userId = userId,
            channel = NotificationChannel.EMAIL,
            type = type,
            content = content,
            status = DeliveryStatus.PENDING
        )
        
        try {
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            
            helper.setFrom(notificationConfig.email.from)
            helper.setTo(recipientEmail)
            helper.setSubject(subject)
            helper.setText(buildEmailBody(type, content), true)
            
            mailSender.send(message)
            
            delivery.status = DeliveryStatus.DELIVERED
            delivery.deliveredAt = Instant.now()
            
            logger.info("Email sent successfully to $recipientEmail for user $userId")
        } catch (e: Exception) {
            delivery.status = DeliveryStatus.FAILED
            delivery.errorMessage = e.message
            logger.error("Failed to send email to $recipientEmail for user $userId", e)
        } finally {
            deliveryRepository.save(delivery)
        }
    }
    
    private fun buildEmailBody(type: NotificationType, content: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .footer { padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>OpenGnosis Notification</h1>
                    </div>
                    <div class="content">
                        <h2>${getNotificationTitle(type)}</h2>
                        <p>$content</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from OpenGnosis. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    private fun getNotificationTitle(type: NotificationType): String {
        return when (type) {
            NotificationType.NEW_GRADE -> "New Grade Posted"
            NotificationType.ATTENDANCE_ALERT -> "Attendance Alert"
            NotificationType.HOMEWORK_ASSIGNED -> "New Homework Assignment"
            NotificationType.SCHEDULE_CHANGE -> "Schedule Change"
            NotificationType.SYSTEM_ANNOUNCEMENT -> "System Announcement"
        }
    }
}
