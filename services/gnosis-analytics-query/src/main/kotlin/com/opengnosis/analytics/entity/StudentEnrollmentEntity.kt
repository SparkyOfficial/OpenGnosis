package com.opengnosis.analytics.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "student_enrollment", indexes = [
    Index(name = "idx_student_enrollment", columnList = "student_id,class_id"),
    Index(name = "idx_student_id_enroll", columnList = "student_id"),
    Index(name = "idx_class_id_enroll", columnList = "class_id")
])
data class StudentEnrollmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @Column(name = "student_id", nullable = false)
    val studentId: UUID,
    
    @Column(name = "class_id", nullable = false)
    val classId: UUID,
    
    @Column(name = "enrollment_date", nullable = false)
    val enrollmentDate: LocalDate,
    
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true
)
