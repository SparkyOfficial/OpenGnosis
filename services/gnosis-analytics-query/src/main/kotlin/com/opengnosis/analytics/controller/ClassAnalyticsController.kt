package com.opengnosis.analytics.controller

import com.opengnosis.analytics.service.ClassAnalyticsService
import com.opengnosis.analytics.service.ClassPerformanceStatistics
import com.opengnosis.domain.ClassPerformanceReadModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/analytics/classes")
class ClassAnalyticsController(
    private val classAnalyticsService: ClassAnalyticsService
) {
    
    @GetMapping("/{classId}/performance")
    fun getClassPerformance(
        @PathVariable classId: UUID,
        @RequestParam subjectId: UUID
    ): ResponseEntity<ClassPerformanceReadModel> {
        val performance = classAnalyticsService.getClassPerformance(classId, subjectId)
        return ResponseEntity.ok(performance)
    }
    
    @GetMapping("/{classId}/statistics")
    fun getClassPerformanceStatistics(
        @PathVariable classId: UUID
    ): ResponseEntity<ClassPerformanceStatistics> {
        val statistics = classAnalyticsService.getClassPerformanceStatistics(classId)
        return ResponseEntity.ok(statistics)
    }
}
