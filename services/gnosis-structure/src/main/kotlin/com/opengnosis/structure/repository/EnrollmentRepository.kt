package com.opengnosis.structure.repository

import com.opengnosis.domain.EnrollmentStatus
import com.opengnosis.structure.domain.entity.EnrollmentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EnrollmentRepository : JpaRepository<EnrollmentEntity, UUID> {
    fun findByStudentId(studentId: UUID): List<EnrollmentEntity>
    fun findByClassId(classId: UUID): List<EnrollmentEntity>
    fun findByClassIdAndStatus(classId: UUID, status: EnrollmentStatus): List<EnrollmentEntity>
    fun findByStudentIdAndStatus(studentId: UUID, status: EnrollmentStatus): List<EnrollmentEntity>
    
    @Query("SELECT e FROM EnrollmentEntity e WHERE e.studentId = :studentId AND e.classId = :classId AND e.status = :status")
    fun findByStudentIdAndClassIdAndStatus(studentId: UUID, classId: UUID, status: EnrollmentStatus): EnrollmentEntity?
}
