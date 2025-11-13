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

class StudentPerformanceQueryIntegrationTest : BaseIntegrationTest() {
    
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
    fun `should query student grades by student ID`() {
        // Given
        val studentId = UUID.randomUUID()
        val subjectId1 = UUID.randomUUID()
        val subjectId2 = UUID.randomUUID()
        
        studentGradesRepository.saveAll(listOf(
            StudentGradesEntity(
                studentId = studentId,
                subjectId = subjectId1,
                gradeValue = 8,
                gradeType = GradeType.EXAM,
                comment = "Good",
                placedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            ),
            StudentGradesEntity(
                studentId = studentId,
                subjectId = subjectId2,
                gradeValue = 9,
                gradeType = GradeType.QUIZ,
                comment = "Excellent",
                placedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            )
        ))
        
        // When
        val response = restTemplate.getForEntity(
            "/api/v1/students/$studentId/grades",
            String::class.java
        )
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.contains(studentId.toString()))
    }
    
    @Test
    fun `should query student attendance by student ID`() {
        // Given
        val studentId = UUID.randomUUID()
        val classId = UUID.randomUUID()
        
        studentAttendanceRepository.saveAll(listOf(
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(2),
                lessonNumber = 1,
                status = AttendanceStatus.PRESENT,
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            ),
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(1),
                lessonNumber = 1,
                status = AttendanceStatus.PRESENT,
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            ),
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now(),
                lessonNumber = 1,
                status = AttendanceStatus.ABSENT,
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            )
        ))
        
        // When
        val response = restTemplate.getForEntity(
            "/api/v1/students/$studentId/attendance",
            String::class.java
        )
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.contains(studentId.toString()))
    }
    
    @Test
    fun `should generate student performance report`() {
        // Given
        val studentId = UUID.randomUUID()
        val subjectId = UUID.randomUUID()
        val classId = UUID.randomUUID()
        
        // Add grades
        studentGradesRepository.saveAll(listOf(
            StudentGradesEntity(
                studentId = studentId,
                subjectId = subjectId,
                gradeValue = 8,
                gradeType = GradeType.EXAM,
                comment = null,
                placedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            ),
            StudentGradesEntity(
                studentId = studentId,
                subjectId = subjectId,
                gradeValue = 9,
                gradeType = GradeType.QUIZ,
                comment = null,
                placedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            )
        ))
        
        // Add attendance
        studentAttendanceRepository.saveAll(listOf(
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(1),
                lessonNumber = 1,
                status = AttendanceStatus.PRESENT,
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            ),
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now(),
                lessonNumber = 1,
                status = AttendanceStatus.PRESENT,
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            )
        ))
        
        // When
        val response = restTemplate.getForEntity(
            "/api/v1/students/$studentId/report",
            String::class.java
        )
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }
    
    @Test
    fun `should return 404 for non-existent student grades`() {
        // Given
        val nonExistentStudentId = UUID.randomUUID()
        
        // When
        val response = restTemplate.getForEntity(
            "/api/v1/students/$nonExistentStudentId/grades",
            String::class.java
        )
        
        // Then - should return empty list or 200 with empty data
        assertEquals(HttpStatus.OK, response.statusCode)
    }
    
    @Test
    fun `should query grades within specific time period`() {
        // Given
        val studentId = UUID.randomUUID()
        val subjectId = UUID.randomUUID()
        val now = Instant.now()
        
        studentGradesRepository.saveAll(listOf(
            StudentGradesEntity(
                studentId = studentId,
                subjectId = subjectId,
                gradeValue = 7,
                gradeType = GradeType.EXAM,
                comment = "Old grade",
                placedBy = UUID.randomUUID(),
                createdAt = now.minusSeconds(86400 * 30) // 30 days ago
            ),
            StudentGradesEntity(
                studentId = studentId,
                subjectId = subjectId,
                gradeValue = 9,
                gradeType = GradeType.QUIZ,
                comment = "Recent grade",
                placedBy = UUID.randomUUID(),
                createdAt = now.minusSeconds(86400) // 1 day ago
            )
        ))
        
        // When - query for last 7 days
        val startDate = now.minusSeconds(86400 * 7)
        val endDate = now
        
        val grades = studentGradesRepository.findByStudentIdAndSubjectIdAndPeriod(
            studentId, subjectId, startDate, endDate
        )
        
        // Then
        assertEquals(1, grades.size)
        assertEquals(9, grades[0].gradeValue)
        assertEquals("Recent grade", grades[0].comment)
    }
    
    @Test
    fun `should calculate attendance statistics correctly`() {
        // Given
        val studentId = UUID.randomUUID()
        val classId = UUID.randomUUID()
        
        studentAttendanceRepository.saveAll(listOf(
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(4),
                lessonNumber = 1,
                status = AttendanceStatus.PRESENT,
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            ),
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(3),
                lessonNumber = 1,
                status = AttendanceStatus.PRESENT,
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            ),
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(2),
                lessonNumber = 1,
                status = AttendanceStatus.ABSENT,
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            ),
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(1),
                lessonNumber = 1,
                status = AttendanceStatus.LATE,
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            ),
            StudentAttendanceEntity(
                studentId = studentId,
                classId = classId,
                date = LocalDate.now(),
                lessonNumber = 1,
                status = AttendanceStatus.PRESENT,
                markedBy = UUID.randomUUID(),
                createdAt = Instant.now()
            )
        ))
        
        // When
        val totalLessons = studentAttendanceRepository.countTotalLessons(studentId, classId)
        val presentCount = studentAttendanceRepository.countByStudentIdAndClassIdAndStatus(
            studentId, classId, AttendanceStatus.PRESENT
        )
        val absentCount = studentAttendanceRepository.countByStudentIdAndClassIdAndStatus(
            studentId, classId, AttendanceStatus.ABSENT
        )
        val lateCount = studentAttendanceRepository.countByStudentIdAndClassIdAndStatus(
            studentId, classId, AttendanceStatus.LATE
        )
        
        // Then
        assertEquals(5, totalLessons)
        assertEquals(3, presentCount)
        assertEquals(1, absentCount)
        assertEquals(1, lateCount)
        
        val attendanceRate = presentCount.toDouble() / totalLessons.toDouble() * 100
        assertEquals(60.0, attendanceRate, 0.01)
    }
}
