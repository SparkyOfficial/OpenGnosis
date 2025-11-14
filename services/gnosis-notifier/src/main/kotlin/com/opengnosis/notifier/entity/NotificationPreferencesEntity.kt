package com.opengnosis.notifier.entity

import com.opengnosis.domain.NotificationType
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "notification_preferences")
data class NotificationPreferencesEntity(
    @Id
    @Column(name = "user_id")
    val userId: UUID,
    
    @Column(name = "email_enabled", nullable = false)
    val emailEnabled: Boolean = true,
    
    @Column(name = "push_enabled", nullable = false)
    val pushEnabled: Boolean = true,
    
    @Column(name = "sms_enabled", nullable = false)
    val smsEnabled: Boolean = false,
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "notification_type_preferences",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Column(name = "notification_type")
    @Enumerated(EnumType.STRING)
    val notificationTypes: Set<NotificationType> = setOf(
        NotificationType.NEW_GRADE,
        NotificationType.ATTENDANCE_ALERT,
        NotificationType.HOMEWORK_ASSIGNED,
        NotificationType.SCHEDULE_CHANGE,
        NotificationType.SYSTEM_ANNOUNCEMENT
    )
)
