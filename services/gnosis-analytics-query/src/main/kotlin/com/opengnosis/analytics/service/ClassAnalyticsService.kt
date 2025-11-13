package com.opengnosis.analytics.service

import com.opengnosis.analytics.repository.StudentEnrollmentRepository
import com.opengnosis.analytics.repository.StudentGradesRepository
import com.opengnosis.domain.ClassPerformanceReadModel
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ClassAnalyticsService(
    private val gradesRepository: StudentGradesRepository,
    private val enrollmentRepository: StudentEnrollmentRepository
) {
    
    @Cacheable(value = ["classPerformance"], key = "#classId + '_' + #subjectId")
    fun getClassPerformance(classId: UUID, subjectId: UUID): ClassPerformanceReadModel {
        // Get all active students in the class
        val enrollments = enrollmentRepository.findByClassIdAndIsActive(classId, true)
        val studentIds = enrollments.map { it.studentId }
        
        if (studentIds.isEmpty()) {
            return ClassPerformanceReadModel(
                classId = classId,
                subjectId = subjectId,
                averageGrade = 0.0,
                studentCount = 0,
                gradeDistribution = emptyMap()
            )
        }
        
        // Get all grades for the subject from students in this class
        val allGrades = studentIds.flatMap { studentId ->
            gradesRepository.findByStudentIdAndSubjectId(studentId, subjectId)
        }
        
        if (allGrades.isEmpty()) {
            return ClassPerformanceReadModel(
                classId = classId,
                subjectId = subjectId,
                averageGrade = 0.0,
                studentCount = studentIds.size,
                gradeDistribution = emptyMap()
            )
        }
        
        // Calculate average grade
        val averageGrade = allGrades.map { it.gradeValue }.average()
        
        // Calculate grade distribution
        val gradeDistribution = allGrades
            .groupBy { it.gradeValue }
            .mapValues { it.value.size }
        
        return ClassPerformanceReadModel(
            classId = classId,
            subjectId = subjectId,
            averageGrade = averageGrade,
            studentCount = studentIds.size,
            gradeDistribution = gradeDistribution
        )
    }
    
    @Cacheable(value = ["classPerformanceStats"], key = "#classId")
    fun getClassPerformanceStatistics(classId: UUID): ClassPerformanceStatistics {
        val enrollments = enrollmentRepository.findByClassIdAndIsActive(classId, true)
        val studentIds = enrollments.map { it.studentId }
        
        if (studentIds.isEmpty()) {
            return ClassPerformanceStatistics(
                classId = classId,
                studentCount = 0,
                totalGrades = 0,
                overallAverage = 0.0,
                subjectAverages = emptyMap()
            )
        }
        
        // Get all grades for all students in the class
        val allGrades = studentIds.flatMap { studentId ->
            gradesRepository.findByStudentId(studentId)
        }
        
        val overallAverage = if (allGrades.isNotEmpty()) {
            allGrades.map { it.gradeValue }.average()
        } else {
            0.0
        }
        
        // Calculate average per subject
        val subjectAverages = allGrades
            .groupBy { it.subjectId }
            .mapValues { (_, grades) ->
                grades.map { it.gradeValue }.average()
            }
        
        return ClassPerformanceStatistics(
            classId = classId,
            studentCount = studentIds.size,
            totalGrades = allGrades.size,
            overallAverage = overallAverage,
            subjectAverages = subjectAverages
        )
    }
}

data class ClassPerformanceStatistics(
    val classId: UUID,
    val studentCount: Int,
    val totalGrades: Int,
    val overallAverage: Double,
    val subjectAverages: Map<UUID, Double>
)
