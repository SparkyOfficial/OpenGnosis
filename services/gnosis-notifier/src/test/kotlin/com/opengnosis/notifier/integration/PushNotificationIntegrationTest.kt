package com.opengnosis.notifier.integration

import com.opengnosis.domain.NotificationType
import com.opengnosis.notifier.repository.DeviceTokenRepository
import com.opengnosis.notifier.repository.NotificationDeliveryRepository
import com.opengnosis.notifier.service.PushNotificationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource
import org.springframework.web.client.RestTemplate
import java.util.UUID

@TestPropertySource(properties = [
    "notification.push.enabled=true",
    "notification.push.fcm.server-key=test-server-key"
])
class PushNotificationIntegrationTest : BaseIntegrationTest() {
    
    @TestConfiguration
    class MockRestTemplateConfig {
        @Bean
        @Primary
        fun mockRestTemplate(): RestTemplate {
            return mockk<RestTemplate>(relaxed = true)
        }
    }
    
    @Autowired
    private lateinit var pushNotificationService: PushNotificationService
    
    @Autowired
    private lateinit var deviceTokenRepository: DeviceTokenRepository
    
    @Autowired
    private lateinit var deliveryRepository: NotificationDeliveryRepository
    
    @Autowired
    private lateinit var restTemplate: RestTemplate
    
    @BeforeEach
    fun setup() {
        deviceTokenRepository.deleteAll()
        deliveryRepository.deleteAll()
    }
    
    @Test
    fun `should register device token successfully`() {
        // Given
        val userId = UUID.randomUUID()
        val token = "test-device-token-123"
        val deviceType = "Android"
        
        // When
        val result = pushNotificationService.registerDeviceToken(userId, token, deviceType)
        
        // Then
        assertNotNull(result)
        assertEquals(userId, result.userId)
        assertEquals(token, result.token)
        assertEquals(deviceType, result.deviceType)
        assertTrue(result.active)
        
        // Verify in database
        val savedToken = deviceTokenRepository.findByToken(token)
        assertNotNull(savedToken)
        assertEquals(userId, savedToken!!.userId)
    }
    
    @Test
    fun `should update existing device token when registering duplicate`() {
        // Given
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val token = "duplicate-token"
        
        // When
        val first = pushNotificationService.registerDeviceToken(userId1, token, "iOS")
        Thread.sleep(100)
        val second = pushNotificationService.registerDeviceToken(userId2, token, "iOS")
        
        // Then
        assertEquals(first.id, second.id)
        assertTrue(second.lastUsedAt?.isAfter(first.lastUsedAt) ?: false)
        
        // Verify only one token exists
        val allTokens = deviceTokenRepository.findAll()
        assertEquals(1, allTokens.size)
    }
    
    @Test
    fun `should send push notification successfully with mock FCM`() {
        // Given
        val userId = UUID.randomUUID()
        val token = "valid-fcm-token"
        pushNotificationService.registerDeviceToken(userId, token, "Android")
        
        val type = NotificationType.NEW_GRADE
        val title = "New Grade"
        val content = "You received a grade of 9"
        
        // Mock successful FCM response
        every { 
            restTemplate.postForEntity(
                "https://fcm.googleapis.com/fcm/send",
                any<HttpEntity<Map<String, Any>>>(),
                Map::class.java
            )
        } returns ResponseEntity(mapOf("success" to 1), HttpStatus.OK)
        
        // When
        pushNotificationService.sendPushNotification(userId, type, title, content)
        
        // Wait for async processing
        Thread.sleep(2000)
        
        // Then
        verify(exactly = 1) {
            restTemplate.postForEntity(
                "https://fcm.googleapis.com/fcm/send",
                any<HttpEntity<Map<String, Any>>>(),
                Map::class.java
            )
        }
        
        // Verify delivery record
        val deliveries = deliveryRepository.findAll()
        assertEquals(1, deliveries.size)
        
        val delivery = deliveries[0]
        assertEquals(userId, delivery.userId)
        assertEquals(com.opengnosis.domain.NotificationChannel.PUSH, delivery.channel)
        assertEquals(type, delivery.type)
        assertEquals(com.opengnosis.domain.DeliveryStatus.DELIVERED, delivery.status)
        assertNotNull(delivery.deliveredAt)
    }
    
    @Test
    fun `should handle FCM failure and record error`() {
        // Given
        val userId = UUID.randomUUID()
        val token = "invalid-fcm-token"
        pushNotificationService.registerDeviceToken(userId, token, "iOS")
        
        // Mock FCM failure
        every { 
            restTemplate.postForEntity(
                "https://fcm.googleapis.com/fcm/send",
                any<HttpEntity<Map<String, Any>>>(),
                Map::class.java
            )
        } returns ResponseEntity(mapOf("error" to "InvalidRegistration"), HttpStatus.BAD_REQUEST)
        
        // When
        pushNotificationService.sendPushNotification(
            userId, NotificationType.HOMEWORK_ASSIGNED, "Homework", "New homework assigned"
        )
        
        // Wait for async processing
        Thread.sleep(2000)
        
        // Then
        val deliveries = deliveryRepository.findAll()
        assertEquals(1, deliveries.size)
        
        val delivery = deliveries[0]
        assertEquals(com.opengnosis.domain.DeliveryStatus.FAILED, delivery.status)
        assertNotNull(delivery.errorMessage)
        assertNull(delivery.deliveredAt)
    }
    
    @Test
    fun `should send to multiple devices for same user`() {
        // Given
        val userId = UUID.randomUUID()
        val token1 = "device-1-token"
        val token2 = "device-2-token"
        
        pushNotificationService.registerDeviceToken(userId, token1, "Android")
        pushNotificationService.registerDeviceToken(userId, token2, "iOS")
        
        // Mock successful FCM responses
        every { 
            restTemplate.postForEntity(
                "https://fcm.googleapis.com/fcm/send",
                any<HttpEntity<Map<String, Any>>>(),
                Map::class.java
            )
        } returns ResponseEntity(mapOf("success" to 1), HttpStatus.OK)
        
        // When
        pushNotificationService.sendPushNotification(
            userId, NotificationType.SCHEDULE_CHANGE, "Schedule Update", "Your schedule changed"
        )
        
        // Wait for async processing
        Thread.sleep(2000)
        
        // Then - should send to both devices
        verify(exactly = 2) {
            restTemplate.postForEntity(
                "https://fcm.googleapis.com/fcm/send",
                any<HttpEntity<Map<String, Any>>>(),
                Map::class.java
            )
        }
        
        val deliveries = deliveryRepository.findAll()
        assertEquals(2, deliveries.size)
        assertTrue(deliveries.all { it.status == com.opengnosis.domain.DeliveryStatus.DELIVERED })
    }
    
    @Test
    fun `should not send push notification when no device tokens registered`() {
        // Given
        val userId = UUID.randomUUID()
        
        // When
        pushNotificationService.sendPushNotification(
            userId, NotificationType.NEW_GRADE, "Grade", "New grade posted"
        )
        
        // Wait for async processing
        Thread.sleep(1000)
        
        // Then - no FCM calls should be made
        verify(exactly = 0) {
            restTemplate.postForEntity(any<String>(), any<HttpEntity<*>>(), any<Class<*>>())
        }
        
        // No delivery records should be created
        val deliveries = deliveryRepository.findAll()
        assertEquals(0, deliveries.size)
    }
    
    @Test
    fun `should deactivate device token`() {
        // Given
        val userId = UUID.randomUUID()
        val token = "token-to-deactivate"
        pushNotificationService.registerDeviceToken(userId, token, "Android")
        
        // When
        pushNotificationService.deactivateDeviceToken(token)
        
        // Then
        val deviceToken = deviceTokenRepository.findByToken(token)
        assertNotNull(deviceToken)
        assertFalse(deviceToken!!.active)
    }
}
