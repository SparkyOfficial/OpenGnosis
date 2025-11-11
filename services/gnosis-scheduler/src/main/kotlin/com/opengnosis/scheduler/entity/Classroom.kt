package com.opengnosis.scheduler.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "classrooms")
data class Classroom(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val schoolId: UUID,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(nullable = false)
    val capacity: Int,
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "classroom_equipment", joinColumns = [JoinColumn(name = "classroom_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment")
    val equipment: Set<Equipment> = emptySet()
)

enum class Equipment {
    PROJECTOR,
    WHITEBOARD,
    SMARTBOARD,
    COMPUTERS,
    LAB_EQUIPMENT,
    SPORTS_EQUIPMENT,
    MUSIC_INSTRUMENTS,
    ART_SUPPLIES
}
