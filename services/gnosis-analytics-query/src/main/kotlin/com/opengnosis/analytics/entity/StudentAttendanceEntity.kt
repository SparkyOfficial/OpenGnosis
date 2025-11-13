package com.opengnosis.analytics.entity

import com.opengnosis.domain.AttendanceStatus
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "student_attendance", indexes = [
    Index(name = "idx_student_class", columnList = "student_id,class_id"),
    Index(name = "idx_student_date", columnList = "student_id,date"),
    Index(name = "idx_class_date", columnList = "class_id,date")
])
data class StudentAttendanceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @Column(name = "student_id", nullable = false)
    val studentId: UUID,
    
    @Column(name = "class_id", nullable = false)
    val classId: UUID,
    
    @Column(name = "date", nullable = false)
    val date: LocalDate,
    
    @Column(name = "lesson_number", nullable = false)
    val lessonNumber: Int,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: AttendanceStatus,
    
    @Column(name = "marked_by", nullable = false)
    val markedBy: UUID,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
