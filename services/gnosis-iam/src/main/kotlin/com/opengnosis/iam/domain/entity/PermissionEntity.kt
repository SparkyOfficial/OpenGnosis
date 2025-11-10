package com.opengnosis.iam.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "permissions", schema = "iam")
class PermissionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, unique = true)
    val name: String,
    
    @Column(nullable = false)
    val resource: String,
    
    @Column(nullable = false)
    val action: String,
    
    @Column
    val description: String? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
