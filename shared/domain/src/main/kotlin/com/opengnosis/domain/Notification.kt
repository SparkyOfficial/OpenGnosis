package com.opengnosis.domain

import java.time.Instant
import java.util.UUID

data class NotificationPreferences(
    val userId: UUID,
    val emailEnabled: Boolean,
    val pushEnabled: Boolean,
    val smsEnabled: Boolean,
    val notificationTypes: Set<NotificationType>
)

enum class NotificationType {
    NEW_GRADE, ATTENDANCE_ALERT, HOMEWORK_ASSIGNED,
    SCHEDULE_CHANGE, SYSTEM_ANNOUNCEMENT
}

data class NotificationDelivery(
    val id: UUID,
    val userId: UUID,
    val channel: NotificationChannel,
    val type: NotificationType,
    val content: String,
    val status: DeliveryStatus,
    val sentAt: Instant,
    val deliveredAt: Instant?
)

enum class NotificationChannel {
    EMAIL, PUSH, SMS
}

enum class DeliveryStatus {
    PENDING, SENT, DELIVERED, FAILED
}
