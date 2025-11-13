package com.opengnosis.analytics.service

import com.opengnosis.analytics.repository.StudentGradesRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TeacherAnalyticsService(
    private val gradesRepository: StudentGradesRepository
) {
    
    @Cacheable(value = ["teacherAnalytics"], key = "#teacherId")
    fun getTeacherStatistics(teacherId: UUID): TeacherStatistics {
        // Get all grades placed by this teacher
        val allGrades = gradesRepository.findAll()
            .filter { it.placedBy == teacherId }
        
        if (allGrades.isEmpty()) {
            return TeacherStatistics(
                teacherId = teacherId,
                totalGradesPlaced = 0,
                averageGradeGiven = 0.0,
                uniqueStudents = 0,
                subjectBreakdown = emptyMap()
            )
        }
        
        val averageGradeGiven = allGrades.map { it.gradeValue }.average()
        val uniqueStudents = allGrades.map { it.studentId }.distinct().size
        
        // Breakdown by subject
        val subjectBreakdown = allGrades
            .groupBy { it.subjectId }
            .mapValues { (_, grades) ->
                SubjectTeachingStats(
                    gradeCount = grades.size,
                    averageGrade = grades.map { it.gradeValue }.average(),
                    uniqueStudents = grades.map { it.studentId }.distinct().size
                )
            }
        
        return TeacherStatistics(
            teacherId = teacherId,
            totalGradesPlaced = allGrades.size,
            averageGradeGiven = averageGradeGiven,
            uniqueStudents = uniqueStudents,
            subjectBreakdown = subjectBreakdown
        )
    }
    
    fun getTeacherClassAssignments(teacherId: UUID): List<TeacherClassAssignment> {
        // This would typically come from the structure service
        // For now, we'll return an empty list as this is a read model
        // In a real implementation, we'd consume TeacherAssignedEvent
        return emptyList()
    }
}

data class TeacherStatistics(
    val teacherId: UUID,
    val totalGradesPlaced: Int,
    val averageGradeGiven: Double,
    val uniqueStudents: Int,
    val subjectBreakdown: Map<UUID, SubjectTeachingStats>
)

data class SubjectTeachingStats(
    val gradeCount: Int,
    val averageGrade: Double,
    val uniqueStudents: Int
)

data class TeacherClassAssignment(
    val teacherId: UUID,
    val classId: UUID,
    val subjectId: UUID,
    val assignedDate: java.time.LocalDate
)
