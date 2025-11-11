package com.opengnosis.scheduler.entity

import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(
    name = "teacher_availability",
    indexes = [Index(name = "idx_teacher_availability_teacher", columnList = "teacher_id")]
)
data class TeacherAvailability(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val teacherId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val dayOfWeek: DayOfWeek,
    
    @Column(nullable = false)
    val startTime: LocalTime,
    
    @Column(nullable = false)
    val endTime: LocalTime,
    
    @Column(nullable = false)
    val available: Boolean = true
)
