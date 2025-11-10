package com.opengnosis.structure.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "academic_years", schema = "structure")
class AcademicYearEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "school_id", nullable = false)
    var schoolId: UUID,
    
    @Column(nullable = false, length = 100)
    var name: String,
    
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,
    
    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AcademicYearStatus = AcademicYearStatus.ACTIVE,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @OneToMany(mappedBy = "academicYear", cascade = [CascadeType.ALL], orphanRemoval = true)
    var terms: MutableList<TermEntity> = mutableListOf()
)

enum class AcademicYearStatus {
    ACTIVE, COMPLETED, ARCHIVED
}
