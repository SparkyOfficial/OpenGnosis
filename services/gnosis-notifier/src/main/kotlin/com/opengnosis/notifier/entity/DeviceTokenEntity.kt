package com.opengnosis.notifier.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "device_tokens")
data class DeviceTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    
    @Column(nullable = false, unique = true)
    val token: String,
    
    @Column(name = "device_type", nullable = false)
    val deviceType: String,
    
    @Column(name = "registered_at", nullable = false)
    val registeredAt: Instant = Instant.now(),
    
    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null,
    
    @Column(nullable = false)
    var active: Boolean = true
)
