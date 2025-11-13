package com.opengnosis.analytics.controller

import com.opengnosis.analytics.service.StudentPerformanceQueryService
import com.opengnosis.analytics.service.StudentPerformanceReport
import com.opengnosis.domain.StudentAttendanceReadModel
import com.opengnosis.domain.StudentGradesReadModel
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/analytics/students")
class StudentPerformanceController(
    private val studentPerformanceQueryService: StudentPerformanceQueryService
) {
    
    @GetMapping("/{studentId}/grades")
    fun getStudentGrades(
        @PathVariable studentId: UUID,
        @RequestParam subjectId: UUID
    ): ResponseEntity<StudentGradesReadModel> {
        val grades = studentPerformanceQueryService.getStudentGrades(studentId, subjectId)
        return ResponseEntity.ok(grades)
    }
    
    @GetMapping("/{studentId}/grades/period")
    fun getStudentGradesByPeriod(
        @PathVariable studentId: UUID,
        @RequestParam subjectId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: Instant
    ): ResponseEntity<StudentGradesReadModel> {
        val grades = studentPerformanceQueryService.getStudentGradesByPeriod(
            studentId, subjectId, startDate, endDate
        )
        return ResponseEntity.ok(grades)
    }
    
    @GetMapping("/{studentId}/attendance")
    fun getStudentAttendance(
        @PathVariable studentId: UUID,
        @RequestParam classId: UUID
    ): ResponseEntity<StudentAttendanceReadModel> {
        val attendance = studentPerformanceQueryService.getStudentAttendance(studentId, classId)
        return ResponseEntity.ok(attendance)
    }
    
    @GetMapping("/{studentId}/attendance/records")
    fun getStudentAttendanceRecords(
        @PathVariable studentId: UUID,
        @RequestParam classId: UUID
    ): ResponseEntity<*> {
        val records = studentPerformanceQueryService.getStudentAttendanceRecords(studentId, classId)
        return ResponseEntity.ok(records)
    }
    
    @GetMapping("/{studentId}/report")
    fun generateStudentPerformanceReport(
        @PathVariable studentId: UUID
    ): ResponseEntity<StudentPerformanceReport> {
        val report = studentPerformanceQueryService.generateStudentPerformanceReport(studentId)
        return ResponseEntity.ok(report)
    }
}
