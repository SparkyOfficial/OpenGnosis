package com.opengnosis.notifier.service

import com.opengnosis.domain.NotificationType
import com.opengnosis.notifier.entity.NotificationPreferencesEntity
import com.opengnosis.notifier.repository.NotificationPreferencesRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NotificationPreferencesService(
    private val preferencesRepository: NotificationPreferencesRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @Transactional(readOnly = true)
    fun getPreferences(userId: UUID): NotificationPreferencesEntity {
        return preferencesRepository.findById(userId)
            .orElseGet { createDefaultPreferences(userId) }
    }
    
    @Transactional
    fun updatePreferences(
        userId: UUID,
        emailEnabled: Boolean?,
        pushEnabled: Boolean?,
        smsEnabled: Boolean?,
        notificationTypes: Set<NotificationType>?
    ): NotificationPreferencesEntity {
        val existing = getPreferences(userId)
        
        val updated = existing.copy(
            emailEnabled = emailEnabled ?: existing.emailEnabled,
            pushEnabled = pushEnabled ?: existing.pushEnabled,
            smsEnabled = smsEnabled ?: existing.smsEnabled,
            notificationTypes = notificationTypes ?: existing.notificationTypes
        )
        
        logger.info("Updating notification preferences for user: $userId")
        return preferencesRepository.save(updated)
    }
    
    @Transactional
    fun createDefaultPreferences(userId: UUID): NotificationPreferencesEntity {
        val defaultPreferences = NotificationPreferencesEntity(
            userId = userId,
            emailEnabled = true,
            pushEnabled = true,
            smsEnabled = false,
            notificationTypes = setOf(
                NotificationType.NEW_GRADE,
                NotificationType.ATTENDANCE_ALERT,
                NotificationType.HOMEWORK_ASSIGNED,
                NotificationType.SCHEDULE_CHANGE,
                NotificationType.SYSTEM_ANNOUNCEMENT
            )
        )
        
        logger.info("Creating default notification preferences for user: $userId")
        return preferencesRepository.save(defaultPreferences)
    }
    
    @Transactional(readOnly = true)
    fun isNotificationEnabled(userId: UUID, type: NotificationType): Boolean {
        val preferences = getPreferences(userId)
        return preferences.notificationTypes.contains(type)
    }
}
