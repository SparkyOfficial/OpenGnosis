package com.opengnosis.notifier.service

import com.opengnosis.domain.NotificationType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NotificationDispatchService(
    private val preferencesService: NotificationPreferencesService,
    private val emailService: EmailNotificationService,
    private val pushService: PushNotificationService,
    private val smsService: SmsNotificationService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun sendNotification(
        userId: UUID,
        type: NotificationType,
        title: String,
        content: String,
        email: String? = null,
        phoneNumber: String? = null
    ) {
        val preferences = preferencesService.getPreferences(userId)
        
        // Check if user wants this type of notification
        if (!preferences.notificationTypes.contains(type)) {
            logger.info("User $userId has disabled notifications of type $type")
            return
        }
        
        // Send via enabled channels
        if (preferences.emailEnabled && email != null) {
            emailService.sendEmail(userId, email, type, title, content)
        }
        
        if (preferences.pushEnabled) {
            pushService.sendPushNotification(userId, type, title, content)
        }
        
        if (preferences.smsEnabled && phoneNumber != null) {
            smsService.sendSms(userId, phoneNumber, type, content)
        }
        
        logger.info("Dispatched notification of type $type to user $userId")
    }
}
