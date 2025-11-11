package com.opengnosis.scheduler.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "schedules")
data class Schedule(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val academicYearId: UUID,
    
    @Column(nullable = false)
    val termId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ScheduleStatus = ScheduleStatus.DRAFT,
    
    @OneToMany(mappedBy = "schedule", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val entries: MutableList<ScheduleEntry> = mutableListOf()
)

enum class ScheduleStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
