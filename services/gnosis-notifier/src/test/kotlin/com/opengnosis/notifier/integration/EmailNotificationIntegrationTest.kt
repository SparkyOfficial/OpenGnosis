package com.opengnosis.notifier.integration

import com.opengnosis.domain.NotificationType
import com.opengnosis.notifier.repository.NotificationDeliveryRepository
import com.opengnosis.notifier.service.EmailNotificationService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import java.util.concurrent.TimeUnit

class EmailNotificationIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var emailNotificationService: EmailNotificationService
    
    @Autowired
    private lateinit var deliveryRepository: NotificationDeliveryRepository
    
    @BeforeEach
    fun setup() {
        deliveryRepository.deleteAll()
        greenMail.purgeEmailFromAllMailboxes()
    }
    
    @Test
    fun `should send email notification successfully`() {
        // Given
        val userId = UUID.randomUUID()
        val recipientEmail = "student@example.com"
        val type = NotificationType.NEW_GRADE
        val subject = "New Grade Posted"
        val content = "You received a grade of 8 in Mathematics"
        
        // When
        emailNotificationService.sendEmail(userId, recipientEmail, type, subject, content)
        
        // Wait for async processing
        greenMail.waitForIncomingEmail(5000, 1)
        
        // Then
        val messages = greenMail.receivedMessages
        assertEquals(1, messages.size)
        
        val message = messages[0]
        assertEquals(subject, message.subject)
        assertEquals("test@opengnosis.com", message.from[0].toString())
        assertTrue(message.allRecipients.any { it.toString() == recipientEmail })
        
        val emailBody = message.content.toString()
        assertTrue(emailBody.contains("New Grade Posted"))
        assertTrue(emailBody.contains(content))
        
        // Verify delivery record
        Thread.sleep(1000) // Wait for DB write
        val deliveries = deliveryRepository.findAll()
        assertEquals(1, deliveries.size)
        
        val delivery = deliveries[0]
        assertEquals(userId, delivery.userId)
        assertEquals(com.opengnosis.domain.NotificationChannel.EMAIL, delivery.channel)
        assertEquals(type, delivery.type)
        assertEquals(com.opengnosis.domain.DeliveryStatus.DELIVERED, delivery.status)
        assertNotNull(delivery.deliveredAt)
    }
    
    @Test
    fun `should handle email sending failure gracefully`() {
        // Given
        val userId = UUID.randomUUID()
        val invalidEmail = "invalid-email-format"
        val type = NotificationType.HOMEWORK_ASSIGNED
        val subject = "New Homework"
        val content = "You have new homework"
        
        // When
        emailNotificationService.sendEmail(userId, invalidEmail, type, subject, content)
        
        // Wait for async processing
        Thread.sleep(2000)
        
        // Then
        val deliveries = deliveryRepository.findAll()
        assertEquals(1, deliveries.size)
        
        val delivery = deliveries[0]
        assertEquals(userId, delivery.userId)
        assertEquals(com.opengnosis.domain.DeliveryStatus.FAILED, delivery.status)
        assertNotNull(delivery.errorMessage)
        assertNull(delivery.deliveredAt)
    }
    
    @Test
    fun `should send multiple emails to different recipients`() {
        // Given
        val user1 = UUID.randomUUID()
        val user2 = UUID.randomUUID()
        val email1 = "parent1@example.com"
        val email2 = "parent2@example.com"
        
        // When
        emailNotificationService.sendEmail(
            user1, email1, NotificationType.ATTENDANCE_ALERT,
            "Attendance Alert", "Your child was absent today"
        )
        
        emailNotificationService.sendEmail(
            user2, email2, NotificationType.SCHEDULE_CHANGE,
            "Schedule Change", "The schedule has been updated"
        )
        
        // Wait for async processing
        greenMail.waitForIncomingEmail(5000, 2)
        
        // Then
        val messages = greenMail.receivedMessages
        assertEquals(2, messages.size)
        
        val recipients = messages.map { it.allRecipients[0].toString() }.toSet()
        assertTrue(recipients.contains(email1))
        assertTrue(recipients.contains(email2))
        
        // Verify delivery records
        Thread.sleep(1000)
        val deliveries = deliveryRepository.findAll()
        assertEquals(2, deliveries.size)
        assertTrue(deliveries.all { it.status == com.opengnosis.domain.DeliveryStatus.DELIVERED })
    }
    
    @Test
    fun `should include proper HTML formatting in email body`() {
        // Given
        val userId = UUID.randomUUID()
        val recipientEmail = "test@example.com"
        val type = NotificationType.SYSTEM_ANNOUNCEMENT
        val subject = "System Announcement"
        val content = "The system will be under maintenance"
        
        // When
        emailNotificationService.sendEmail(userId, recipientEmail, type, subject, content)
        
        // Wait for async processing
        greenMail.waitForIncomingEmail(5000, 1)
        
        // Then
        val message = greenMail.receivedMessages[0]
        val emailBody = message.content.toString()
        
        assertTrue(emailBody.contains("<!DOCTYPE html>"))
        assertTrue(emailBody.contains("<html>"))
        assertTrue(emailBody.contains("OpenGnosis Notification"))
        assertTrue(emailBody.contains(content))
        assertTrue(emailBody.contains("System Announcement"))
    }
}
