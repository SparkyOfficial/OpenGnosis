package com.opengnosis.structure.integration

import com.opengnosis.domain.EnrollmentStatus
import com.opengnosis.structure.dto.CreateClassRequest
import com.opengnosis.structure.dto.CreateSchoolRequest
import com.opengnosis.structure.dto.EnrollStudentRequest
import com.opengnosis.structure.dto.UnenrollStudentRequest
import com.opengnosis.structure.repository.EnrollmentRepository
import com.opengnosis.structure.service.ClassService
import com.opengnosis.structure.service.EnrollmentService
import com.opengnosis.structure.service.SchoolService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Transactional
class EnrollmentIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var schoolService: SchoolService
    
    @Autowired
    private lateinit var classService: ClassService
    
    @Autowired
    private lateinit var enrollmentService: EnrollmentService
    
    @Autowired
    private lateinit var enrollmentRepository: EnrollmentRepository
    
    @Test
    fun `should enroll student successfully`() {
        // Given
        val school = createTestSchool()
        val classEntity = createTestClass(school.id)
        val studentId = UUID.randomUUID()
        
        val enrollRequest = EnrollStudentRequest(
            studentId = studentId,
            classId = classEntity.id,
            enrollmentDate = LocalDate.now()
        )
        
        // When
        val enrollment = enrollmentService.enrollStudent(enrollRequest)
        
        // Then
        assertNotNull(enrollment.id)
        assertEquals(studentId, enrollment.studentId)
        assertEquals(classEntity.id, enrollment.classId)
        assertEquals(EnrollmentStatus.ACTIVE, enrollment.status)
        assertNotNull(enrollment.enrollmentDate)
        assertNull(enrollment.unenrollmentDate)
        
        // Verify in database
        val savedEnrollment = enrollmentRepository.findById(enrollment.id)
        assertTrue(savedEnrollment.isPresent)
        assertEquals(studentId, savedEnrollment.get().studentId)
    }
    
    @Test
    fun `should retrieve enrollments by class id`() {
        // Given
        val school = createTestSchool()
        val classEntity = createTestClass(school.id)
        val student1Id = UUID.randomUUID()
        val student2Id = UUID.randomUUID()
        
        enrollmentService.enrollStudent(EnrollStudentRequest(student1Id, classEntity.id))
        enrollmentService.enrollStudent(EnrollStudentRequest(student2Id, classEntity.id))
        
        // When
        val enrollments = enrollmentRepository.findByClassId(classEntity.id)
        
        // Then
        assertEquals(2, enrollments.size)
        assertTrue(enrollments.any { it.studentId == student1Id })
        assertTrue(enrollments.any { it.studentId == student2Id })
    }
    
    @Test
    fun `should retrieve class composition`() {
        // Given
        val school = createTestSchool()
        val classEntity = createTestClass(school.id)
        val student1Id = UUID.randomUUID()
        val student2Id = UUID.randomUUID()
        val student3Id = UUID.randomUUID()
        
        enrollmentService.enrollStudent(EnrollStudentRequest(student1Id, classEntity.id))
        enrollmentService.enrollStudent(EnrollStudentRequest(student2Id, classEntity.id))
        enrollmentService.enrollStudent(EnrollStudentRequest(student3Id, classEntity.id))
        
        // When
        val composition = enrollmentService.getClassComposition(classEntity.id)
        
        // Then
        assertEquals(classEntity.id, composition.classId)
        assertEquals(3, composition.totalStudents)
        assertEquals(3, composition.students.size)
        assertTrue(composition.students.all { it.status == EnrollmentStatus.ACTIVE })
    }
    
    @Test
    fun `should unenroll student successfully`() {
        // Given
        val school = createTestSchool()
        val classEntity = createTestClass(school.id)
        val studentId = UUID.randomUUID()
        
        val enrollment = enrollmentService.enrollStudent(
            EnrollStudentRequest(studentId, classEntity.id)
        )
        
        // When
        val unenrolled = enrollmentService.unenrollStudent(
            enrollment.id,
            UnenrollStudentRequest(reason = "Student transferred")
        )
        
        // Then
        assertEquals(EnrollmentStatus.WITHDRAWN, unenrolled.status)
        assertNotNull(unenrolled.unenrollmentDate)
        
        // Verify in database
        val savedEnrollment = enrollmentRepository.findById(enrollment.id)
        assertTrue(savedEnrollment.isPresent)
        assertEquals(EnrollmentStatus.WITHDRAWN, savedEnrollment.get().status)
    }
    
    @Test
    fun `should prevent duplicate active enrollment`() {
        // Given
        val school = createTestSchool()
        val classEntity = createTestClass(school.id)
        val studentId = UUID.randomUUID()
        
        val enrollRequest = EnrollStudentRequest(studentId, classEntity.id)
        enrollmentService.enrollStudent(enrollRequest)
        
        // When/Then - attempting to enroll same student again should fail
        assertThrows(Exception::class.java) {
            enrollmentService.enrollStudent(enrollRequest)
        }
    }
    
    @Test
    fun `should allow re-enrollment after unenrollment`() {
        // Given
        val school = createTestSchool()
        val classEntity = createTestClass(school.id)
        val studentId = UUID.randomUUID()
        
        val firstEnrollment = enrollmentService.enrollStudent(
            EnrollStudentRequest(studentId, classEntity.id)
        )
        enrollmentService.unenrollStudent(
            firstEnrollment.id,
            UnenrollStudentRequest(reason = "Temporary leave")
        )
        
        // When
        val secondEnrollment = enrollmentService.enrollStudent(
            EnrollStudentRequest(studentId, classEntity.id)
        )
        
        // Then
        assertNotNull(secondEnrollment.id)
        assertNotEquals(firstEnrollment.id, secondEnrollment.id)
        assertEquals(EnrollmentStatus.ACTIVE, secondEnrollment.status)
        
        // Verify both enrollments exist in database
        val allEnrollments = enrollmentRepository.findByStudentId(studentId)
        assertEquals(2, allEnrollments.size)
        assertEquals(1, allEnrollments.count { it.status == EnrollmentStatus.ACTIVE })
        assertEquals(1, allEnrollments.count { it.status == EnrollmentStatus.WITHDRAWN })
    }
    
    @Test
    fun `should retrieve active enrollments only`() {
        // Given
        val school = createTestSchool()
        val classEntity = createTestClass(school.id)
        val student1Id = UUID.randomUUID()
        val student2Id = UUID.randomUUID()
        val student3Id = UUID.randomUUID()
        
        enrollmentService.enrollStudent(EnrollStudentRequest(student1Id, classEntity.id))
        val enrollment2 = enrollmentService.enrollStudent(EnrollStudentRequest(student2Id, classEntity.id))
        enrollmentService.enrollStudent(EnrollStudentRequest(student3Id, classEntity.id))
        
        // Unenroll one student
        enrollmentService.unenrollStudent(
            enrollment2.id,
            UnenrollStudentRequest(reason = "Withdrawn")
        )
        
        // When
        val activeEnrollments = enrollmentRepository.findByClassIdAndStatus(
            classEntity.id,
            EnrollmentStatus.ACTIVE
        )
        
        // Then
        assertEquals(2, activeEnrollments.size)
        assertTrue(activeEnrollments.none { it.studentId == student2Id })
    }
    
    // Helper methods
    private fun createTestSchool() = schoolService.createSchool(
        CreateSchoolRequest(
            name = "Test School ${UUID.randomUUID()}",
            address = "Test Address",
            principalId = UUID.randomUUID()
        )
    )
    
    private fun createTestClass(schoolId: UUID) = classService.createClass(
        CreateClassRequest(
            schoolId = schoolId,
            academicYearId = UUID.randomUUID(),
            name = "Test Class ${UUID.randomUUID()}",
            grade = 10,
            classTeacherId = UUID.randomUUID(),
            capacity = 30
        )
    )
}
