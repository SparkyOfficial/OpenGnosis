package com.opengnosis.analytics.service

import com.opengnosis.analytics.repository.StudentAttendanceRepository
import com.opengnosis.analytics.repository.StudentGradesRepository
import com.opengnosis.domain.GradeEntry
import com.opengnosis.domain.StudentAttendanceReadModel
import com.opengnosis.domain.StudentGradesReadModel
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class StudentPerformanceQueryService(
    private val gradesRepository: StudentGradesRepository,
    private val attendanceRepository: StudentAttendanceRepository,
    private val gradesReadModelService: GradesReadModelService,
    private val attendanceReadModelService: AttendanceReadModelService
) {
    
    @Cacheable(value = ["studentGrades"], key = "#studentId + '_' + #subjectId")
    fun getStudentGrades(studentId: UUID, subjectId: UUID): StudentGradesReadModel {
        val grades = gradesRepository.findByStudentIdAndSubjectId(studentId, subjectId)
        
        val gradeEntries = grades.map { entity ->
            GradeEntry(
                id = entity.id!!,
                value = entity.gradeValue,
                type = entity.gradeType,
                comment = entity.comment,
                createdAt = entity.createdAt
            )
        }
        
        val average = gradesReadModelService.calculateAverageGrade(studentId, subjectId)
        
        return StudentGradesReadModel(
            studentId = studentId,
            subjectId = subjectId,
            grades = gradeEntries,
            average = average,
            lastUpdated = Instant.now()
        )
    }
    
    @Cacheable(value = ["studentGradesByPeriod"], key = "#studentId + '_' + #subjectId + '_' + #startDate + '_' + #endDate")
    fun getStudentGradesByPeriod(
        studentId: UUID,
        subjectId: UUID,
        startDate: Instant,
        endDate: Instant
    ): StudentGradesReadModel {
        val grades = gradesRepository.findByStudentIdAndSubjectIdAndPeriod(
            studentId, subjectId, startDate, endDate
        )
        
        val gradeEntries = grades.map { entity ->
            GradeEntry(
                id = entity.id!!,
                value = entity.gradeValue,
                type = entity.gradeType,
                comment = entity.comment,
                createdAt = entity.createdAt
            )
        }
        
        val average = if (gradeEntries.isNotEmpty()) {
            gradeEntries.map { it.value }.average()
        } else {
            0.0
        }
        
        return StudentGradesReadModel(
            studentId = studentId,
            subjectId = subjectId,
            grades = gradeEntries,
            average = average,
            lastUpdated = Instant.now()
        )
    }
    
    @Cacheable(value = ["studentAttendance"], key = "#studentId + '_' + #classId")
    fun getStudentAttendance(studentId: UUID, classId: UUID): StudentAttendanceReadModel {
        val statistics = attendanceReadModelService.getAttendanceStatistics(studentId, classId)
        
        return StudentAttendanceReadModel(
            studentId = studentId,
            classId = classId,
            totalLessons = statistics.totalLessons,
            presentCount = statistics.presentCount,
            absentCount = statistics.absentCount,
            lateCount = statistics.lateCount,
            attendanceRate = statistics.attendanceRate
        )
    }
    
    @Cacheable(value = ["studentAttendanceRecords"], key = "#studentId + '_' + #classId")
    fun getStudentAttendanceRecords(studentId: UUID, classId: UUID): List<AttendanceRecord> {
        val records = attendanceRepository.findByStudentIdAndClassId(studentId, classId)
        
        return records.map { entity ->
            AttendanceRecord(
                id = entity.id!!,
                studentId = entity.studentId,
                classId = entity.classId,
                date = entity.date,
                lessonNumber = entity.lessonNumber,
                status = entity.status.name,
                markedBy = entity.markedBy,
                createdAt = entity.createdAt
            )
        }
    }
    
    @Cacheable(value = ["studentPerformanceReport"], key = "#studentId")
    fun generateStudentPerformanceReport(studentId: UUID): StudentPerformanceReport {
        val allGrades = gradesRepository.findByStudentId(studentId)
        val allAttendance = attendanceRepository.findByStudentId(studentId)
        
        // Group grades by subject
        val gradesBySubject = allGrades.groupBy { it.subjectId }
        
        val subjectPerformances = gradesBySubject.map { (subjectId, grades) ->
            val gradeValues = grades.map { it.gradeValue }
            SubjectPerformance(
                subjectId = subjectId,
                averageGrade = gradeValues.average(),
                gradeCount = grades.size,
                minGrade = gradeValues.minOrNull() ?: 0,
                maxGrade = gradeValues.maxOrNull() ?: 0
            )
        }
        
        // Group attendance by class
        val attendanceByClass = allAttendance.groupBy { it.classId }
        
        val classAttendances = attendanceByClass.map { (classId, records) ->
            val statistics = attendanceReadModelService.getAttendanceStatistics(studentId, classId)
            ClassAttendance(
                classId = classId,
                totalLessons = statistics.totalLessons,
                presentCount = statistics.presentCount,
                attendanceRate = statistics.attendanceRate
            )
        }
        
        return StudentPerformanceReport(
            studentId = studentId,
            subjectPerformances = subjectPerformances,
            classAttendances = classAttendances,
            overallAverageGrade = if (allGrades.isNotEmpty()) {
                allGrades.map { it.gradeValue }.average()
            } else {
                0.0
            },
            generatedAt = Instant.now()
        )
    }
}

data class AttendanceRecord(
    val id: UUID,
    val studentId: UUID,
    val classId: UUID,
    val date: LocalDate,
    val lessonNumber: Int,
    val status: String,
    val markedBy: UUID,
    val createdAt: Instant
)

data class StudentPerformanceReport(
    val studentId: UUID,
    val subjectPerformances: List<SubjectPerformance>,
    val classAttendances: List<ClassAttendance>,
    val overallAverageGrade: Double,
    val generatedAt: Instant
)

data class SubjectPerformance(
    val subjectId: UUID,
    val averageGrade: Double,
    val gradeCount: Int,
    val minGrade: Int,
    val maxGrade: Int
)

data class ClassAttendance(
    val classId: UUID,
    val totalLessons: Int,
    val presentCount: Int,
    val attendanceRate: Double
)
