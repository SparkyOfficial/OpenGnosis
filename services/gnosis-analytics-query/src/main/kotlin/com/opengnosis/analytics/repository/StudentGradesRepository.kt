package com.opengnosis.analytics.repository

import com.opengnosis.analytics.entity.StudentGradesEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface StudentGradesRepository : JpaRepository<StudentGradesEntity, UUID> {
    
    fun findByStudentIdAndSubjectId(studentId: UUID, subjectId: UUID): List<StudentGradesEntity>
    
    fun findByStudentId(studentId: UUID): List<StudentGradesEntity>
    
    fun findBySubjectId(subjectId: UUID): List<StudentGradesEntity>
    
    @Query("SELECT g FROM StudentGradesEntity g WHERE g.studentId = :studentId AND g.subjectId = :subjectId AND g.createdAt BETWEEN :startDate AND :endDate")
    fun findByStudentIdAndSubjectIdAndPeriod(
        studentId: UUID,
        subjectId: UUID,
        startDate: Instant,
        endDate: Instant
    ): List<StudentGradesEntity>
    
    @Query("SELECT AVG(g.gradeValue) FROM StudentGradesEntity g WHERE g.studentId = :studentId AND g.subjectId = :subjectId")
    fun calculateAverageGrade(studentId: UUID, subjectId: UUID): Double?
}
