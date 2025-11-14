package com.opengnosis.notifier.service

import com.opengnosis.domain.DeliveryStatus
import com.opengnosis.domain.NotificationChannel
import com.opengnosis.domain.NotificationType
import com.opengnosis.notifier.config.NotificationConfig
import com.opengnosis.notifier.entity.NotificationDeliveryEntity
import com.opengnosis.notifier.repository.NotificationDeliveryRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class SmsNotificationService(
    private val notificationConfig: NotificationConfig,
    private val deliveryRepository: NotificationDeliveryRepository,
    private val restTemplate: RestTemplate = RestTemplate()
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @Async
    fun sendSms(
        userId: UUID,
        phoneNumber: String,
        type: NotificationType,
        content: String
    ) {
        if (!notificationConfig.sms.enabled) {
            logger.info("SMS notifications are disabled")
            return
        }
        
        val delivery = NotificationDeliveryEntity(
            userId = userId,
            channel = NotificationChannel.SMS,
            type = type,
            content = content,
            status = DeliveryStatus.PENDING
        )
        
        var attempt = 0
        val maxAttempts = notificationConfig.retry.maxAttempts
        var success = false
        
        while (attempt < maxAttempts && !success) {
            try {
                if (attempt > 0) {
                    val backoffTime = notificationConfig.retry.initialInterval * 
                        Math.pow(notificationConfig.retry.backoffMultiplier.toDouble(), (attempt - 1).toDouble()).toLong()
                    Thread.sleep(backoffTime)
                    logger.info("Retrying SMS send (attempt ${attempt + 1}/$maxAttempts) after ${backoffTime}ms")
                }
                
                sendViaTwilio(phoneNumber, content)
                
                delivery.status = DeliveryStatus.DELIVERED
                delivery.deliveredAt = Instant.now()
                success = true
                
                logger.info("SMS sent successfully to $phoneNumber for user $userId")
            } catch (e: Exception) {
                attempt++
                delivery.retryCount = attempt
                delivery.errorMessage = e.message
                
                if (attempt >= maxAttempts) {
                    delivery.status = DeliveryStatus.FAILED
                    logger.error("Failed to send SMS to $phoneNumber after $maxAttempts attempts", e)
                } else {
                    logger.warn("SMS send attempt $attempt failed, will retry: ${e.message}")
                }
            }
        }
        
        deliveryRepository.save(delivery)
    }
    
    private fun sendViaTwilio(phoneNumber: String, content: String) {
        val twilioConfig = notificationConfig.sms.twilio
        val accountSid = twilioConfig.accountSid
        val authToken = twilioConfig.authToken
        val fromNumber = twilioConfig.fromNumber
        
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            throw IllegalStateException("Twilio configuration is incomplete")
        }
        
        val url = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"
        
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        
        val auth = "$accountSid:$authToken"
        val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray())
        headers.set("Authorization", "Basic $encodedAuth")
        
        val body: MultiValueMap<String, String> = LinkedMultiValueMap()
        body.add("From", fromNumber)
        body.add("To", phoneNumber)
        body.add("Body", content)
        
        val request = HttpEntity(body, headers)
        val response = restTemplate.postForEntity(url, request, Map::class.java)
        
        if (!response.statusCode.is2xxSuccessful) {
            throw RuntimeException("Twilio API returned status: ${response.statusCode}")
        }
    }
}
