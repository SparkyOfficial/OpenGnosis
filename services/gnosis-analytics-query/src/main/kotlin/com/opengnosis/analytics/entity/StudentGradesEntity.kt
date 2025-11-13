package com.opengnosis.analytics.entity

import com.opengnosis.domain.GradeType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "student_grades", indexes = [
    Index(name = "idx_student_subject", columnList = "student_id,subject_id"),
    Index(name = "idx_student_id", columnList = "student_id"),
    Index(name = "idx_subject_id", columnList = "subject_id")
])
data class StudentGradesEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @Column(name = "student_id", nullable = false)
    val studentId: UUID,
    
    @Column(name = "subject_id", nullable = false)
    val subjectId: UUID,
    
    @Column(name = "grade_value", nullable = false)
    val gradeValue: Int,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "grade_type", nullable = false)
    val gradeType: GradeType,
    
    @Column(name = "comment", columnDefinition = "TEXT")
    val comment: String? = null,
    
    @Column(name = "placed_by", nullable = false)
    val placedBy: UUID,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "last_updated", nullable = false)
    val lastUpdated: Instant = Instant.now()
)
