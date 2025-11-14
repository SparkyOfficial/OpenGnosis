package com.opengnosis.notifier.service

import com.opengnosis.domain.DeliveryStatus
import com.opengnosis.domain.NotificationChannel
import com.opengnosis.domain.NotificationType
import com.opengnosis.notifier.config.NotificationConfig
import com.opengnosis.notifier.entity.DeviceTokenEntity
import com.opengnosis.notifier.entity.NotificationDeliveryEntity
import com.opengnosis.notifier.repository.DeviceTokenRepository
import com.opengnosis.notifier.repository.NotificationDeliveryRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.util.UUID

@Service
class PushNotificationService(
    private val notificationConfig: NotificationConfig,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val deliveryRepository: NotificationDeliveryRepository,
    private val restTemplate: RestTemplate = RestTemplate()
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val fcmUrl = "https://fcm.googleapis.com/fcm/send"
    
    fun registerDeviceToken(userId: UUID, token: String, deviceType: String): DeviceTokenEntity {
        val existing = deviceTokenRepository.findByToken(token)
        if (existing != null) {
            existing.lastUsedAt = Instant.now()
            existing.active = true
            return deviceTokenRepository.save(existing)
        }
        
        val newToken = DeviceTokenEntity(
            userId = userId,
            token = token,
            deviceType = deviceType
        )
        
        logger.info("Registering device token for user: $userId")
        return deviceTokenRepository.save(newToken)
    }
    
    @Async
    fun sendPushNotification(
        userId: UUID,
        type: NotificationType,
        title: String,
        content: String
    ) {
        if (!notificationConfig.push.enabled) {
            logger.info("Push notifications are disabled")
            return
        }
        
        val tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId)
        if (tokens.isEmpty()) {
            logger.warn("No active device tokens found for user: $userId")
            return
        }
        
        tokens.forEach { deviceToken ->
            sendToDevice(userId, deviceToken, type, title, content)
        }
    }
    
    private fun sendToDevice(
        userId: UUID,
        deviceToken: DeviceTokenEntity,
        type: NotificationType,
        title: String,
        content: String
    ) {
        val delivery = NotificationDeliveryEntity(
            userId = userId,
            channel = NotificationChannel.PUSH,
            type = type,
            content = content,
            status = DeliveryStatus.PENDING
        )
        
        try {
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            headers.set("Authorization", "key=${notificationConfig.push.fcm.serverKey}")
            
            val payload = mapOf(
                "to" to deviceToken.token,
                "notification" to mapOf(
                    "title" to title,
                    "body" to content,
                    "sound" to "default"
                ),
                "data" to mapOf(
                    "type" to type.name,
                    "userId" to userId.toString()
                )
            )
            
            val request = HttpEntity(payload, headers)
            val response = restTemplate.postForEntity(fcmUrl, request, Map::class.java)
            
            if (response.statusCode.is2xxSuccessful) {
                delivery.status = DeliveryStatus.DELIVERED
                delivery.deliveredAt = Instant.now()
                deviceToken.lastUsedAt = Instant.now()
                deviceTokenRepository.save(deviceToken)
                logger.info("Push notification sent successfully to device ${deviceToken.id}")
            } else {
                delivery.status = DeliveryStatus.FAILED
                delivery.errorMessage = "FCM returned status: ${response.statusCode}"
                logger.error("Failed to send push notification: ${response.statusCode}")
            }
        } catch (e: Exception) {
            delivery.status = DeliveryStatus.FAILED
            delivery.errorMessage = e.message
            
            // Deactivate token if it's invalid
            if (e.message?.contains("InvalidRegistration") == true || 
                e.message?.contains("NotRegistered") == true) {
                deviceToken.active = false
                deviceTokenRepository.save(deviceToken)
                logger.warn("Deactivated invalid device token: ${deviceToken.id}")
            }
            
            logger.error("Failed to send push notification to device ${deviceToken.id}", e)
        } finally {
            deliveryRepository.save(delivery)
        }
    }
    
    fun deactivateDeviceToken(token: String) {
        val deviceToken = deviceTokenRepository.findByToken(token)
        if (deviceToken != null) {
            deviceToken.active = false
            deviceTokenRepository.save(deviceToken)
            logger.info("Deactivated device token: ${deviceToken.id}")
        }
    }
}
