package com.opengnosis.structure.repository

import com.opengnosis.structure.domain.entity.ClassEntity
import com.opengnosis.structure.domain.entity.ClassStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ClassRepository : JpaRepository<ClassEntity, UUID> {
    fun findBySchoolId(schoolId: UUID): List<ClassEntity>
    fun findBySchoolIdAndStatus(schoolId: UUID, status: ClassStatus): List<ClassEntity>
    fun findByAcademicYearId(academicYearId: UUID): List<ClassEntity>
    fun findByClassTeacherId(teacherId: UUID): List<ClassEntity>
    
    @Query("SELECT c FROM ClassEntity c WHERE c.schoolId = :schoolId AND c.academicYearId = :academicYearId AND c.name = :name")
    fun findBySchoolIdAndAcademicYearIdAndName(schoolId: UUID, academicYearId: UUID, name: String): ClassEntity?
}
