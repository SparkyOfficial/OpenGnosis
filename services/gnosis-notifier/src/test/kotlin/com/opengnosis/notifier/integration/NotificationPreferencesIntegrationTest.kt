package com.opengnosis.notifier.integration

import com.opengnosis.domain.NotificationType
import com.opengnosis.notifier.entity.NotificationPreferencesEntity
import com.opengnosis.notifier.repository.NotificationDeliveryRepository
import com.opengnosis.notifier.repository.NotificationPreferencesRepository
import com.opengnosis.notifier.service.NotificationDispatchService
import com.opengnosis.notifier.service.NotificationPreferencesService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class NotificationPreferencesIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var preferencesService: NotificationPreferencesService
    
    @Autowired
    private lateinit var preferencesRepository: NotificationPreferencesRepository
    
    @Autowired
    private lateinit var notificationDispatchService: NotificationDispatchService
    
    @Autowired
    private lateinit var deliveryRepository: NotificationDeliveryRepository
    
    @BeforeEach
    fun setup() {
        preferencesRepository.deleteAll()
        deliveryRepository.deleteAll()
        greenMail.purgeEmailFromAllMailboxes()
    }
    
    @Test
    fun `should create default preferences for new user`() {
        // Given
        val userId = UUID.randomUUID()
        
        // When
        val preferences = preferencesService.getPreferences(userId)
        
        // Then
        assertNotNull(preferences)
        assertEquals(userId, preferences.userId)
        assertTrue(preferences.emailEnabled)
        assertTrue(preferences.pushEnabled)
        assertFalse(preferences.smsEnabled)
        assertEquals(5, preferences.notificationTypes.size)
        assertTrue(preferences.notificationTypes.contains(NotificationType.NEW_GRADE))
        assertTrue(preferences.notificationTypes.contains(NotificationType.ATTENDANCE_ALERT))
        assertTrue(preferences.notificationTypes.contains(NotificationType.HOMEWORK_ASSIGNED))
        assertTrue(preferences.notificationTypes.contains(NotificationType.SCHEDULE_CHANGE))
        assertTrue(preferences.notificationTypes.contains(NotificationType.SYSTEM_ANNOUNCEMENT))
        
        // Verify saved in database
        val saved = preferencesRepository.findById(userId)
        assertTrue(saved.isPresent)
    }
    
    @Test
    fun `should update user preferences`() {
        // Given
        val userId = UUID.randomUUID()
        preferencesService.createDefaultPreferences(userId)
        
        // When
        val updated = preferencesService.updatePreferences(
            userId = userId,
            emailEnabled = false,
            pushEnabled = true,
            smsEnabled = true,
            notificationTypes = setOf(NotificationType.NEW_GRADE, NotificationType.ATTENDANCE_ALERT)
        )
        
        // Then
        assertFalse(updated.emailEnabled)
        assertTrue(updated.pushEnabled)
        assertTrue(updated.smsEnabled)
        assertEquals(2, updated.notificationTypes.size)
        assertTrue(updated.notificationTypes.contains(NotificationType.NEW_GRADE))
        assertTrue(updated.notificationTypes.contains(NotificationType.ATTENDANCE_ALERT))
        assertFalse(updated.notificationTypes.contains(NotificationType.HOMEWORK_ASSIGNED))
        
        // Verify in database
        val fromDb = preferencesRepository.findById(userId).get()
        assertEquals(updated.emailEnabled, fromDb.emailEnabled)
        assertEquals(updated.notificationTypes, fromDb.notificationTypes)
    }
    
    @Test
    fun `should respect notification type preferences when dispatching`() {
        // Given
        val userId = UUID.randomUUID()
        
        // Create preferences that only allow NEW_GRADE notifications
        val preferences = NotificationPreferencesEntity(
            userId = userId,
            emailEnabled = true,
            pushEnabled = false,
            smsEnabled = false,
            notificationTypes = setOf(NotificationType.NEW_GRADE)
        )
        preferencesRepository.save(preferences)
        
        // When - try to send a HOMEWORK_ASSIGNED notification (not in preferences)
        notificationDispatchService.sendNotification(
            userId = userId,
            type = NotificationType.HOMEWORK_ASSIGNED,
            title = "New Homework",
            content = "You have new homework",
            email = "student@example.com",
            phoneNumber = null
        )
        
        // Wait for async processing
        Thread.sleep(2000)
        
        // Then - no notification should be sent
        val messages = greenMail.receivedMessages
        assertEquals(0, messages.size)
        
        val deliveries = deliveryRepository.findAll()
        assertEquals(0, deliveries.size)
    }
    
    @Test
    fun `should send notification when type is in preferences`() {
        // Given
        val userId = UUID.randomUUID()
        
        // Create preferences that allow NEW_GRADE notifications
        val preferences = NotificationPreferencesEntity(
            userId = userId,
            emailEnabled = true,
            pushEnabled = false,
            smsEnabled = false,
            notificationTypes = setOf(NotificationType.NEW_GRADE, NotificationType.ATTENDANCE_ALERT)
        )
        preferencesRepository.save(preferences)
        
        // When - send a NEW_GRADE notification (in preferences)
        notificationDispatchService.sendNotification(
            userId = userId,
            type = NotificationType.NEW_GRADE,
            title = "New Grade Posted",
            content = "You received a grade of 9",
            email = "student@example.com",
            phoneNumber = null
        )
        
        // Wait for async processing
        greenMail.waitForIncomingEmail(5000, 1)
        
        // Then - notification should be sent
        val messages = greenMail.receivedMessages
        assertEquals(1, messages.size)
        
        Thread.sleep(1000)
        val deliveries = deliveryRepository.findAll()
        assertEquals(1, deliveries.size)
        assertEquals(NotificationType.NEW_GRADE, deliveries[0].type)
    }
    
    @Test
    fun `should respect channel preferences when dispatching`() {
        // Given
        val userId = UUID.randomUUID()
        
        // Create preferences with email disabled but push enabled
        val preferences = NotificationPreferencesEntity(
            userId = userId,
            emailEnabled = false,
            pushEnabled = true,
            smsEnabled = false,
            notificationTypes = setOf(NotificationType.NEW_GRADE)
        )
        preferencesRepository.save(preferences)
        
        // When - send notification with email provided
        notificationDispatchService.sendNotification(
            userId = userId,
            type = NotificationType.NEW_GRADE,
            title = "New Grade",
            content = "Grade posted",
            email = "student@example.com",
            phoneNumber = null
        )
        
        // Wait for async processing
        Thread.sleep(2000)
        
        // Then - no email should be sent (email disabled in preferences)
        val messages = greenMail.receivedMessages
        assertEquals(0, messages.size)
        
        // But push notification would be attempted (if device tokens exist)
        // Since we don't have device tokens, no delivery records for push either
    }
    
    @Test
    fun `should allow partial preference updates`() {
        // Given
        val userId = UUID.randomUUID()
        preferencesService.createDefaultPreferences(userId)
        
        // When - update only email preference
        val updated = preferencesService.updatePreferences(
            userId = userId,
            emailEnabled = false,
            pushEnabled = null,
            smsEnabled = null,
            notificationTypes = null
        )
        
        // Then - only email should be changed
        assertFalse(updated.emailEnabled)
        assertTrue(updated.pushEnabled) // unchanged
        assertFalse(updated.smsEnabled) // unchanged
        assertEquals(5, updated.notificationTypes.size) // unchanged
    }
    
    @Test
    fun `should check if specific notification type is enabled`() {
        // Given
        val userId = UUID.randomUUID()
        val preferences = NotificationPreferencesEntity(
            userId = userId,
            emailEnabled = true,
            pushEnabled = true,
            smsEnabled = false,
            notificationTypes = setOf(NotificationType.NEW_GRADE, NotificationType.ATTENDANCE_ALERT)
        )
        preferencesRepository.save(preferences)
        
        // When & Then
        assertTrue(preferencesService.isNotificationEnabled(userId, NotificationType.NEW_GRADE))
        assertTrue(preferencesService.isNotificationEnabled(userId, NotificationType.ATTENDANCE_ALERT))
        assertFalse(preferencesService.isNotificationEnabled(userId, NotificationType.HOMEWORK_ASSIGNED))
        assertFalse(preferencesService.isNotificationEnabled(userId, NotificationType.SCHEDULE_CHANGE))
    }
    
    @Test
    fun `should handle multiple users with different preferences`() {
        // Given
        val user1 = UUID.randomUUID()
        val user2 = UUID.randomUUID()
        
        val prefs1 = NotificationPreferencesEntity(
            userId = user1,
            emailEnabled = true,
            pushEnabled = false,
            smsEnabled = false,
            notificationTypes = setOf(NotificationType.NEW_GRADE)
        )
        
        val prefs2 = NotificationPreferencesEntity(
            userId = user2,
            emailEnabled = true,
            pushEnabled = false,
            smsEnabled = false,
            notificationTypes = setOf(NotificationType.HOMEWORK_ASSIGNED)
        )
        
        preferencesRepository.save(prefs1)
        preferencesRepository.save(prefs2)
        
        // When - send same notification type to both users
        notificationDispatchService.sendNotification(
            userId = user1,
            type = NotificationType.NEW_GRADE,
            title = "Grade",
            content = "New grade",
            email = "user1@example.com",
            phoneNumber = null
        )
        
        notificationDispatchService.sendNotification(
            userId = user2,
            type = NotificationType.NEW_GRADE,
            title = "Grade",
            content = "New grade",
            email = "user2@example.com",
            phoneNumber = null
        )
        
        // Wait for async processing
        greenMail.waitForIncomingEmail(5000, 1)
        Thread.sleep(1000)
        
        // Then - only user1 should receive notification (has NEW_GRADE in preferences)
        val messages = greenMail.receivedMessages
        assertEquals(1, messages.size)
        assertTrue(messages[0].allRecipients[0].toString().contains("user1@example.com"))
    }
}
