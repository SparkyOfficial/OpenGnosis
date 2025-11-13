package com.opengnosis.analytics.controller

import com.opengnosis.analytics.service.AggregationService
import com.opengnosis.analytics.service.DashboardStatistics
import com.opengnosis.analytics.service.GradeTrend
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/analytics/aggregations")
class AggregationController(
    private val aggregationService: AggregationService
) {
    
    @GetMapping("/grade-distribution/subject/{subjectId}")
    fun getGradeDistributionBySubject(
        @PathVariable subjectId: String
    ): ResponseEntity<Map<Int, Long>> {
        val distribution = aggregationService.getGradeDistributionBySubject(subjectId)
        return ResponseEntity.ok(distribution)
    }
    
    @GetMapping("/grade-distribution/student/{studentId}")
    fun getGradeDistributionByStudent(
        @PathVariable studentId: String
    ): ResponseEntity<Map<Int, Long>> {
        val distribution = aggregationService.getGradeDistributionByStudent(studentId)
        return ResponseEntity.ok(distribution)
    }
    
    @GetMapping("/grade-trends")
    fun getGradeTrends(
        @RequestParam studentId: String,
        @RequestParam subjectId: String
    ): ResponseEntity<List<GradeTrend>> {
        val trends = aggregationService.getGradeTrendsByStudent(studentId, subjectId)
        return ResponseEntity.ok(trends)
    }
    
    @GetMapping("/average-grades-by-subject")
    fun getAverageGradesBySubject(): ResponseEntity<Map<String, Double>> {
        val averages = aggregationService.getAverageGradesBySubject()
        return ResponseEntity.ok(averages)
    }
    
    @GetMapping("/dashboard")
    fun getDashboardStatistics(): ResponseEntity<DashboardStatistics> {
        val statistics = aggregationService.getDashboardStatistics()
        return ResponseEntity.ok(statistics)
    }
}
