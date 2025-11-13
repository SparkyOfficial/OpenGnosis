package com.opengnosis.analytics.controller

import com.opengnosis.analytics.service.TeacherAnalyticsService
import com.opengnosis.analytics.service.TeacherClassAssignment
import com.opengnosis.analytics.service.TeacherStatistics
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/analytics/teachers")
class TeacherAnalyticsController(
    private val teacherAnalyticsService: TeacherAnalyticsService
) {
    
    @GetMapping("/{teacherId}/statistics")
    fun getTeacherStatistics(
        @PathVariable teacherId: UUID
    ): ResponseEntity<TeacherStatistics> {
        val statistics = teacherAnalyticsService.getTeacherStatistics(teacherId)
        return ResponseEntity.ok(statistics)
    }
    
    @GetMapping("/{teacherId}/classes")
    fun getTeacherClassAssignments(
        @PathVariable teacherId: UUID
    ): ResponseEntity<List<TeacherClassAssignment>> {
        val assignments = teacherAnalyticsService.getTeacherClassAssignments(teacherId)
        return ResponseEntity.ok(assignments)
    }
}
