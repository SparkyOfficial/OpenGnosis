package com.opengnosis.journal.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "command_audit_log")
data class CommandAuditLog(
    @Id
    val id: UUID,
    
    @Column(nullable = false)
    val commandType: String,
    
    @Column(nullable = false)
    val issuedBy: UUID,
    
    @Column(nullable = false)
    val timestamp: Instant,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CommandStatus,
    
    @Column(columnDefinition = "TEXT")
    val payload: String,
    
    @Column(columnDefinition = "TEXT")
    var errorMessage: String? = null,
    
    @Column(nullable = false)
    val processedAt: Instant = Instant.now()
)

enum class CommandStatus {
    ACCEPTED, REJECTED, FAILED
}
