package com.opengnosis.structure.domain.entity

import com.opengnosis.domain.SchoolStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "schools", schema = "structure")
class SchoolEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    var name: String,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    var address: String,
    
    @Column(name = "principal_id", nullable = false)
    var principalId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SchoolStatus = SchoolStatus.ACTIVE,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
}
