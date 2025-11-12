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
    val capacity: Int
)
