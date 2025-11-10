package com.opengnosis.structure.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "classes",
    schema = "structure",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_class_name_year", columnNames = ["school_id", "academic_year_id", "name"])
    ]
)
class ClassEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "school_id", nullable = false)
    var schoolId: UUID,
    
    @Column(name = "academic_year_id", nullable = false)
    var academicYearId: UUID,
    
    @Column(nullable = false, length = 100)
    var name: String,
    
    @Column(nullable = false)
    var grade: Int,
    
    @Column(name = "class_teacher_id", nullable = false)
    var classTeacherId: UUID,
    
    @Column(nullable = false)
    var capacity: Int = 30,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ClassStatus = ClassStatus.ACTIVE,
    
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

enum class ClassStatus {
    ACTIVE, INACTIVE, DELETED
}
