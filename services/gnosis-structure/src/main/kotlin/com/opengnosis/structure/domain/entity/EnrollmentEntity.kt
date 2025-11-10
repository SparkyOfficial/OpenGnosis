package com.opengnosis.structure.domain.entity

import com.opengnosis.domain.EnrollmentStatus
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "enrollments",
    schema = "structure",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_student_class_active", columnNames = ["student_id", "class_id", "status"])
    ]
)
class EnrollmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "student_id", nullable = false)
    var studentId: UUID,
    
    @Column(name = "class_id", nullable = false)
    var classId: UUID,
    
    @Column(name = "enrollment_date", nullable = false)
    var enrollmentDate: LocalDate = LocalDate.now(),
    
    @Column(name = "unenrollment_date")
    var unenrollmentDate: LocalDate? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: EnrollmentStatus = EnrollmentStatus.ACTIVE,
    
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
