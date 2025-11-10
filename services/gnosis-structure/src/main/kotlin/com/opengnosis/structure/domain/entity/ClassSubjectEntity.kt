package com.opengnosis.structure.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "class_subjects",
    schema = "structure",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_class_subject", columnNames = ["class_id", "subject_id"])
    ]
)
class ClassSubjectEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "class_id", nullable = false)
    var classId: UUID,
    
    @Column(name = "subject_id", nullable = false)
    var subjectId: UUID,
    
    @Column(name = "teacher_id", nullable = false)
    var teacherId: UUID,
    
    @Column(name = "hours_per_week", nullable = false)
    var hoursPerWeek: Int = 1,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
