package com.opengnosis.analytics.repository

import com.opengnosis.analytics.entity.StudentAttendanceEntity
import com.opengnosis.domain.AttendanceStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface StudentAttendanceRepository : JpaRepository<StudentAttendanceEntity, UUID> {
    
    fun findByStudentIdAndClassId(studentId: UUID, classId: UUID): List<StudentAttendanceEntity>
    
    fun findByStudentId(studentId: UUID): List<StudentAttendanceEntity>
    
    fun findByClassId(classId: UUID): List<StudentAttendanceEntity>
    
    @Query("SELECT a FROM StudentAttendanceEntity a WHERE a.studentId = :studentId AND a.classId = :classId AND a.date BETWEEN :startDate AND :endDate")
    fun findByStudentIdAndClassIdAndPeriod(
        studentId: UUID,
        classId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<StudentAttendanceEntity>
    
    @Query("SELECT COUNT(a) FROM StudentAttendanceEntity a WHERE a.studentId = :studentId AND a.classId = :classId AND a.status = :status")
    fun countByStudentIdAndClassIdAndStatus(
        studentId: UUID,
        classId: UUID,
        status: AttendanceStatus
    ): Long
    
    @Query("SELECT COUNT(a) FROM StudentAttendanceEntity a WHERE a.studentId = :studentId AND a.classId = :classId")
    fun countTotalLessons(studentId: UUID, classId: UUID): Long
}
