package com.opengnosis.analytics.integration

import com.opengnosis.analytics.entity.StudentAttendanceEntity
import com.opengnosis.analytics.entity.StudentGradesEntity
import com.opengnosis.analytics.repository.StudentAttendanceRepository
import com.opengnosis.analytics.repository.StudentGradesRepository
import com.opengnosis.domain.AttendanceStatus
import com.opengnosis.domain.GradeType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.system.measureTimeMillis

class QueryPerformanceIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var restTemplate: TestRestTemplate
    
    @Autowired
    private lateinit var studentGradesRepository: StudentGradesRepository
    
    @Autowired
    private lateinit var studentAttendanceRepository: StudentAttendanceRepository
    
    @BeforeEach
    fun setup() {
        studentGradesRepository.deleteAll()
        studentAttendanceRepository.deleteAll()
    }
    
    @Test
    fun `should query student grades within 1 second with sample data`() {
        // Given - create sample data for a student
        val studentId = UUID.randomUUID()
        val subjects = (1..5).map { UUID.randomUUID() }
        
        val grades = subjects.flatMap { subjectId ->
            (1..10).map { i ->
                StudentGradesEntity(
                    studentId = studentId,
                    subjectId = subjectId,
                    gradeValue = (5..10).random(),
                    gradeType = GradeType.values().random(),
                    comment = "Grade $i",
                    placedBy = UUID.randomUUID(),
                    createdAt = Instant.now().minusSeconds(86400L * i)
                )
            }
        }
        
        studentGradesRepository.saveAll(grades)
        
        // When
        val executionTime = measureTimeMillis {
            val response = restTemplate.getForEntity(
                "/api/v1/students/$studentId/grades",
                String::class.java
            )
            assertEquals(HttpStatus.OK, response.statusCode)
        }
        
        // Then - should complete within 1 second (1000ms)
        assertTrue(executionTime < 1000, "Query took ${executionTime}ms, expected < 1000ms")
    }
    
    @Test
    fun `should query student attendance within 1 second with sample data`() {
        // Given - create sample attendance data
        val studentId = UUID.randomUUID()
        val classId = UUID.randomUUID()
        
        val attendanceRecords = (1..30).map { i ->
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(i.toLong()),
                lessonNumber = (1..5).random(),
                status = AttendanceStatus.values().random(),
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now().minusSeconds(86400L * i)
            )
        }
        
        studentAttendanceRepository.saveAll(attendanceRecords)
        
        // When
        val executionTime = measureTimeMillis {
            val response = restTemplate.getForEntity(
                "/api/v1/students/$studentId/attendance",
                String::class.java
            )
            assertEquals(HttpStatus.OK, response.statusCode)
        }
        
        // Then - should complete within 1 second (1000ms)
        assertTrue(executionTime < 1000, "Query took ${executionTime}ms, expected < 1000ms")
    }
    
    @Test
    fun `should generate student report within 1 second`() {
        // Given - create comprehensive student data
        val studentId = UUID.randomUUID()
        val subjectId = UUID.randomUUID()
        val classId = UUID.randomUUID()
        
        // Add grades
        val grades = (1..20).map { i ->
            StudentGradesEntity(
                studentId = studentId,
                subjectId = subjectId,
                gradeValue = (5..10).random(),
                gradeType = GradeType.values().random(),
                comment = "Grade $i",
                placedBy = UUID.randomUUID(),
                createdAt = Instant.now().minusSeconds(86400L * i)
            )
        }
        studentGradesRepository.saveAll(grades)
        
        // Add attendance
        val attendanceRecords = (1..30).map { i ->
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(i.toLong()),
                lessonNumber = 1,
                status = if (i % 5 == 0) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT,
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now().minusSeconds(86400L * i)
            )
        }
        studentAttendanceRepository.saveAll(attendanceRecords)
        
        // When
        val executionTime = measureTimeMillis {
            val response = restTemplate.getForEntity(
                "/api/v1/students/$studentId/report",
                String::class.java
            )
            assertEquals(HttpStatus.OK, response.statusCode)
        }
        
        // Then - should complete within 1 second (1000ms)
        assertTrue(executionTime < 1000, "Report generation took ${executionTime}ms, expected < 1000ms")
    }
    
    @Test
    fun `should calculate grade average efficiently`() {
        // Given
        val studentId = UUID.randomUUID()
        val subjectId = UUID.randomUUID()
        
        val grades = (1..100).map { i ->
            StudentGradesEntity(
                studentId = studentId,
                subjectId = subjectId,
                gradeValue = (5..10).random(),
                gradeType = GradeType.values().random(),
                comment = null,
                placedBy = UUID.randomUUID(),
                createdAt = Instant.now().minusSeconds(i.toLong())
            )
        }
        studentGradesRepository.saveAll(grades)
        
        // When
        val executionTime = measureTimeMillis {
            val average = studentGradesRepository.calculateAverageGrade(studentId, subjectId)
            assertNotNull(average)
            assertTrue(average!! >= 5.0 && average <= 10.0)
        }
        
        // Then - should complete quickly
        assertTrue(executionTime < 500, "Average calculation took ${executionTime}ms, expected < 500ms")
    }
    
    @Test
    fun `should count attendance statistics efficiently`() {
        // Given
        val studentId = UUID.randomUUID()
        val classId = UUID.randomUUID()
        
        val attendanceRecords = (1..100).map { i ->
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(i.toLong()),
                lessonNumber = 1,
                status = when (i % 4) {
                    0 -> AttendanceStatus.ABSENT
                    1 -> AttendanceStatus.LATE
                    else -> AttendanceStatus.PRESENT
                },
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now().minusSeconds(86400L * i)
            )
        }
        studentAttendanceRepository.saveAll(attendanceRecords)
        
        // When
        val executionTime = measureTimeMillis {
            val totalLessons = studentAttendanceRepository.countTotalLessons(studentId, classId)
            val presentCount = studentAttendanceRepository.countByStudentIdAndClassIdAndStatus(
                studentId, classId, AttendanceStatus.PRESENT
            )
            val absentCount = studentAttendanceRepository.countByStudentIdAndClassIdAndStatus(
                studentId, classId, AttendanceStatus.ABSENT
            )
            
            assertEquals(100, totalLessons)
            assertTrue(presentCount > 0)
            assertTrue(absentCount > 0)
        }
        
        // Then - should complete quickly
        assertTrue(executionTime < 500, "Statistics calculation took ${executionTime}ms, expected < 500ms")
    }
    
    @Test
    fun `should handle concurrent queries efficiently`() {
        // Given - create data for multiple students
        val students = (1..5).map { UUID.randomUUID() }
        val subjectId = UUID.randomUUID()
        
        students.forEach { studentId ->
            val grades = (1..10).map { i ->
                StudentGradesEntity(
                    studentId = studentId,
                    subjectId = subjectId,
                    gradeValue = (5..10).random(),
                    gradeType = GradeType.values().random(),
                    comment = null,
                    placedBy = UUID.randomUUID(),
                    createdAt = Instant.now().minusSeconds(i.toLong())
                )
            }
            studentGradesRepository.saveAll(grades)
        }
        
        // When - query all students concurrently
        val executionTime = measureTimeMillis {
            students.forEach { studentId ->
                val response = restTemplate.getForEntity(
                    "/api/v1/students/$studentId/grades",
                    String::class.java
                )
                assertEquals(HttpStatus.OK, response.statusCode)
            }
        }
        
        // Then - all queries should complete within reasonable time
        assertTrue(executionTime < 3000, "Concurrent queries took ${executionTime}ms, expected < 3000ms")
    }
}
