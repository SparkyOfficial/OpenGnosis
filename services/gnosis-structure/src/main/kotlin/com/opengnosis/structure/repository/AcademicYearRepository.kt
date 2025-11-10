package com.opengnosis.structure.repository

import com.opengnosis.structure.domain.entity.AcademicYearEntity
import com.opengnosis.structure.domain.entity.AcademicYearStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AcademicYearRepository : JpaRepository<AcademicYearEntity, UUID> {
    fun findBySchoolId(schoolId: UUID): List<AcademicYearEntity>
    fun findBySchoolIdAndStatus(schoolId: UUID, status: AcademicYearStatus): List<AcademicYearEntity>
}
