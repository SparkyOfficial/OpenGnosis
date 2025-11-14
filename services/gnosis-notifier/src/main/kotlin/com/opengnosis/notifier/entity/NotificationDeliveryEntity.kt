package com.opengnosis.notifier.entity

import com.opengnosis.domain.DeliveryStatus
import com.opengnosis.domain.NotificationChannel
import com.opengnosis.domain.NotificationType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notification_delivery")
data class NotificationDeliveryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val channel: NotificationChannel,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationType,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DeliveryStatus = DeliveryStatus.PENDING,
    
    @Column(name = "sent_at", nullable = false)
    val sentAt: Instant = Instant.now(),
    
    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null,
    
    @Column(name = "error_message")
    var errorMessage: String? = null,
    
    @Column(name = "retry_count")
    var retryCount: Int = 0
)
