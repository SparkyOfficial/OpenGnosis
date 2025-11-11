package com.opengnosis.scheduler.entity

import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(
    name = "schedule_entries",
    indexes = [
        Index(name = "idx_schedule_entry_schedule", columnList = "schedule_id"),
        Index(name = "idx_schedule_entry_class", columnList = "class_id"),
        Index(name = "idx_schedule_entry_teacher", columnList = "teacher_id"),
        Index(name = "idx_schedule_entry_classroom", columnList = "classroom_id")
    ]
)
data class ScheduleEntry(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    val schedule: Schedule,
    
    @Column(nullable = false)
    val classId: UUID,
    
    @Column(nullable = false)
    val subjectId: UUID,
    
    @Column(nullable = false)
    val teacherId: UUID,
    
    @Column(nullable = false)
    val classroomId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val dayOfWeek: DayOfWeek,
    
    @Column(nullable = false)
    val startTime: LocalTime,
    
    @Column(nullable = false)
    val endTime: LocalTime
)
