package com.opengnosis.analytics.integration

import com.opengnosis.analytics.repository.StudentAttendanceRepository
import com.opengnosis.analytics.repository.StudentEnrollmentRepository
import com.opengnosis.analytics.repository.StudentGradesRepository
import com.opengnosis.domain.AttendanceStatus
import com.opengnosis.domain.GradeType
import com.opengnosis.events.AttendanceMarkedEvent
import com.opengnosis.events.GradePlacedEvent
import com.opengnosis.events.StudentEnrolledEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit

class EventConsumptionIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    
    @Autowired
    private lateinit var studentGradesRepository: StudentGradesRepository
    
    @Autowired
    private lateinit var studentAttendanceRepository: StudentAttendanceRepository
    
    @Autowired
    private lateinit var studentEnrollmentRepository: StudentEnrollmentRepository
    
    @BeforeEach
    fun setup() {
        studentGradesRepository.deleteAll()
        studentAttendanceRepository.deleteAll()
        studentEnrollmentRepository.deleteAll()
    }
    
    @Test
    fun `should consume GradePlacedEvent and update read model`() {
        // Given
        val studentId = UUID.randomUUID()
        val subjectId = UUID.randomUUID()
        val event = GradePlacedEvent(
            aggregateId = UUID.randomUUID(),
            studentId = studentId,
            subjectId = subjectId,
            gradeValue = 8,
            gradeType = GradeType.EXAM,
            comment = "Good performance",
            placedBy = UUID.randomUUID()
        )
        
        // When
        val sendResult: SendResult<String, Any> = kafkaTemplate.send("grade-placed", event.eventId.toString(), event)
            .get(10, TimeUnit.SECONDS)
        
        assertNotNull(sendResult)
        
        // Wait for event processing
        Thread.sleep(2000)
        
        // Then
        val grades = studentGradesRepository.findByStudentIdAndSubjectId(studentId, subjectId)
        assertEquals(1, grades.size)
        
        val grade = grades[0]
        assertEquals(studentId, grade.studentId)
        assertEquals(subjectId, grade.subjectId)
        assertEquals(8, grade.gradeValue)
        assertEquals(GradeType.EXAM, grade.gradeType)
        assertEquals("Good performance", grade.comment)
    }
    
    @Test
    fun `should consume multiple GradePlacedEvents and calculate average`() {
        // Given
        val studentId = UUID.randomUUID()
        val subjectId = UUID.randomUUID()
        
        val events = listOf(
            GradePlacedEvent(
                aggregateId = UUID.randomUUID(),
                studentId = studentId,
                subjectId = subjectId,
                gradeValue = 8,
                gradeType = GradeType.EXAM,
                comment = null,
                placedBy = UUID.randomUUID()
            ),
            GradePlacedEvent(
                aggregateId = UUID.randomUUID(),
                studentId = studentId,
                subjectId = subjectId,
                gradeValue = 9,
                gradeType = GradeType.QUIZ,
                comment = null,
                placedBy = UUID.randomUUID()
            ),
            GradePlacedEvent(
                aggregateId = UUID.randomUUID(),
                studentId = studentId,
                subjectId = subjectId,
                gradeValue = 7,
                gradeType = GradeType.HOMEWORK,
                comment = null,
                placedBy = UUID.randomUUID()
            )
        )
        
        // When
        events.forEach { event ->
            kafkaTemplate.send("grade-placed", event.eventId.toString(), event)
                .get(10, TimeUnit.SECONDS)
        }
        
        // Wait for event processing
        Thread.sleep(3000)
        
        // Then
        val grades = studentGradesRepository.findByStudentIdAndSubjectId(studentId, subjectId)
        assertEquals(3, grades.size)
        
        val average = studentGradesRepository.calculateAverageGrade(studentId, subjectId)
        assertNotNull(average)
        assertEquals(8.0, average!!, 0.01)
    }
    
    @Test
    fun `should consume AttendanceMarkedEvent and update read model`() {
        // Given
        val studentId = UUID.randomUUID()
        val classId = UUID.randomUUID()
        val event = AttendanceMarkedEvent(
            aggregateId = UUID.randomUUID(),
            studentId = studentId,
            classId = classId,
            date = LocalDate.now(),
            lessonNumber = 1,
            status = AttendanceStatus.PRESENT,
            markedBy = UUID.randomUUID()
        )
        
        // When
        kafkaTemplate.send("attendance-marked", event.eventId.toString(), event)
            .get(10, TimeUnit.SECONDS)
        
        // Wait for event processing
        Thread.sleep(2000)
        
        // Then
        val attendanceRecords = studentAttendanceRepository.findByStudentIdAndClassId(studentId, classId)
        assertEquals(1, attendanceRecords.size)
        
        val attendance = attendanceRecords[0]
        assertEquals(studentId, attendance.studentId)
        assertEquals(classId, attendance.classId)
        assertEquals(LocalDate.now(), attendance.date)
        assertEquals(1, attendance.lessonNumber)
        assertEquals(AttendanceStatus.PRESENT, attendance.status)
    }
    
    @Test
    fun `should consume multiple AttendanceMarkedEvents and calculate statistics`() {
        // Given
        val studentId = UUID.randomUUID()
        val classId = UUID.randomUUID()
        
        val events = listOf(
            AttendanceMarkedEvent(
                aggregateId = UUID.randomUUID(),
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(4),
                lessonNumber = 1,
                status = AttendanceStatus.PRESENT,
                markedBy = UUID.randomUUID()
            ),
            AttendanceMarkedEvent(
                aggregateId = UUID.randomUUID(),
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(3),
                lessonNumber = 1,
                status = AttendanceStatus.PRESENT,
                markedBy = UUID.randomUUID()
            ),
            AttendanceMarkedEvent(
                aggregateId = UUID.randomUUID(),
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(2),
                lessonNumber = 1,
                status = AttendanceStatus.ABSENT,
                markedBy = UUID.randomUUID()
            ),
            AttendanceMarkedEvent(
                aggregateId = UUID.randomUUID(),
                studentId = studentId,
                classId = classId,
                date = LocalDate.now().minusDays(1),
                lessonNumber = 1,
                status = AttendanceStatus.PRESENT,
                markedBy = UUID.randomUUID()
            ),
            AttendanceMarkedEvent(
                aggregateId = UUID.randomUUID(),
                studentId = studentId,
                classId = classId,
                date = LocalDate.now(),
                lessonNumber = 1,
                status = AttendanceStatus.LATE,
                markedBy = UUID.randomUUID()
            )
        )
        
        // When
        events.forEach { event ->
            kafkaTemplate.send("attendance-marked", event.eventId.toString(), event)
                .get(10, TimeUnit.SECONDS)
        }
        
        // Wait for event processing
        Thread.sleep(3000)
        
        // Then
        val attendanceRecords = studentAttendanceRepository.findByStudentIdAndClassId(studentId, classId)
        assertEquals(5, attendanceRecords.size)
        
        val totalLessons = studentAttendanceRepository.countTotalLessons(studentId, classId)
        assertEquals(5, totalLessons)
        
        val presentCount = studentAttendanceRepository.countByStudentIdAndClassIdAndStatus(
            studentId, classId, AttendanceStatus.PRESENT
        )
        assertEquals(3, presentCount)
        
        val absentCount = studentAttendanceRepository.countByStudentIdAndClassIdAndStatus(
            studentId, classId, AttendanceStatus.ABSENT
        )
        assertEquals(1, absentCount)
        
        val lateCount = studentAttendanceRepository.countByStudentIdAndClassIdAndStatus(
            studentId, classId, AttendanceStatus.LATE
        )
        assertEquals(1, lateCount)
    }
    
    @Test
    fun `should consume StudentEnrolledEvent and create enrollment record`() {
        // Given
        val studentId = UUID.randomUUID()
        val classId = UUID.randomUUID()
        val event = StudentEnrolledEvent(
            aggregateId = UUID.randomUUID(),
            studentId = studentId,
            classId = classId,
            enrollmentDate = LocalDate.now()
        )
        
        // When
        kafkaTemplate.send("student-enrolled", event.eventId.toString(), event)
            .get(10, TimeUnit.SECONDS)
        
        // Wait for event processing
        Thread.sleep(2000)
        
        // Then
        val enrollment = studentEnrollmentRepository.findByStudentIdAndClassId(studentId, classId)
        assertNotNull(enrollment)
        assertEquals(studentId, enrollment!!.studentId)
        assertEquals(classId, enrollment.classId)
        assertEquals(LocalDate.now(), enrollment.enrollmentDate)
        assertTrue(enrollment.isActive)
    }
    
    @Test
    fun `should handle duplicate StudentEnrolledEvent idempotently`() {
        // Given
        val studentId = UUID.randomUUID()
        val classId = UUID.randomUUID()
        val event = StudentEnrolledEvent(
            aggregateId = UUID.randomUUID(),
            studentId = studentId,
            classId = classId,
            enrollmentDate = LocalDate.now()
        )
        
        // When - send the same event twice
        kafkaTemplate.send("student-enrolled", event.eventId.toString(), event)
            .get(10, TimeUnit.SECONDS)
        
        Thread.sleep(2000)
        
        kafkaTemplate.send("student-enrolled", event.eventId.toString(), event)
            .get(10, TimeUnit.SECONDS)
        
        Thread.sleep(2000)
        
        // Then - should only have one enrollment record
        val enrollments = studentEnrollmentRepository.findAll()
        val matchingEnrollments = enrollments.filter { 
            it.studentId == studentId && it.classId == classId 
        }
        assertEquals(1, matchingEnrollments.size)
    }
}
