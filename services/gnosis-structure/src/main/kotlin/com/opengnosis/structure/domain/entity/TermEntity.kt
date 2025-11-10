package com.opengnosis.structure.domain.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "terms", schema = "structure")
class TermEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID = UUID.randomUUID(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    var academicYear: AcademicYearEntity,
    
    @Column(nullable = false, length = 100)
    var name: String,
    
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,
    
    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,
    
    @Column(name = "term_number", nullable = false)
    var termNumber: Int,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
