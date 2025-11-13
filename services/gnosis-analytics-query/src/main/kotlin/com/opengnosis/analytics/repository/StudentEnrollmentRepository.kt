package com.opengnosis.analytics.repository

import com.opengnosis.analytics.entity.StudentEnrollmentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StudentEnrollmentRepository : JpaRepository<StudentEnrollmentEntity, UUID> {
    
    fun findByStudentIdAndClassId(studentId: UUID, classId: UUID): StudentEnrollmentEntity?
    
    fun findByStudentIdAndIsActive(studentId: UUID, isActive: Boolean): List<StudentEnrollmentEntity>
    
    fun findByClassIdAndIsActive(classId: UUID, isActive: Boolean): List<StudentEnrollmentEntity>
}
