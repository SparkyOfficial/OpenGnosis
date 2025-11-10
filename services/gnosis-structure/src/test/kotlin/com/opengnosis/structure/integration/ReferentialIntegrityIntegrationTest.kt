package com.opengnosis.structure.integration

import com.opengnosis.domain.EnrollmentStatus
import com.opengnosis.structure.domain.entity.ClassStatus
import com.opengnosis.structure.dto.CreateClassRequest
import com.opengnosis.structure.dto.CreateSchoolRequest
import com.opengnosis.structure.dto.EnrollStudentRequest
import com.opengnosis.structure.dto.UnenrollStudentRequest
import com.opengnosis.structure.repository.ClassRepository
import com.opengnosis.structure.repository.EnrollmentRepository
import com.opengnosis.structure.service.ClassService
import com.opengnosis.structure.service.EnrollmentService
import com.opengnosis.structure.service.SchoolService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class ReferentialIntegrityIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var schoolService: SchoolService
    
    @Autowired
    private lateinit var classService: ClassService
    
    @Autowired
    private lateinit var enrollmentService: EnrollmentService
    
    @Autowired
    private lateinit var classRepository: ClassRepository
    
    @Autowired
    private lateinit var enrollmentRepository: EnrollmentRepository
    
    @Test
    fun `should soft delete class when deleted`() {
        // Given
        val school = createTestSchool()
        val classEntity = createTestClass(school.id)
        
        // When
        classService.deleteClass(classEntity.id)
        
        // Then
        val deletedClass = classRepository.findById(classEntity.id)
        assertTrue(deletedClass.isPresent)
        assertEquals(ClassStatus.DELETED, deletedClass.get().status)
    }
    
    @Test
    fun `should unenroll all students when class is deleted`() {
        // Given
        val school = createTestSchool()
        val classEntity = createTestClass(school.id)
        val student1Id = UUID.randomUUID()
        val student2Id = UUID.randomUUID()
        val student3Id = UUID.randomUUID()
        
        enrollmentService.enrollStudent(EnrollStudentRequest(student1Id, classEntity.id))
        enrollmentService.enrollStudent(EnrollStudentRequest(student2Id, classEntity.id))
        enrollmentService.enrollStudent(EnrollStudentRequest(student3Id, classEntity.id))
        
        // Verify students are enrolled
        val beforeDelete = enrollmentRepository.findByClassIdAndStatus(
            classEntity.id,
            EnrollmentStatus.ACTIVE
        )
        assertEquals(3, beforeDelete.size)
        
        // When
        classService.deleteClass(classEntity.id)
        
        // Then
        val afterDelete = enrollmentRepository.findByClassIdAndStatus(
            classEntity.id,
            EnrollmentStatus.ACTIVE
        )
        assertEquals(0, afterDelete.size)
        
        // Verify enrollments are withdrawn, not deleted
        val allEnrollments = enrollmentRepository.findByClassId(classEntity.id)
        assertEquals(3, allEnrollments.size)
        assertTrue(allEnrollments.all { it.status == EnrollmentStatus.WITHDRAWN })
    }
    
    @Test
    fun `should maintain referential integrity between school and classes`() {
        // Given
        val school = createTestSchool()
        val class1 = createTestClass(school.id)
        val class2 = createTestClass(school.id)
        
        // When
        val classes = classRepository.findBySchoolId(school.id)
        
        // Then
        assertEquals(2, classes.size)
        assertTrue(classes.all { it.schoolId == school.id })
    }
    
    @Test
    fun `should maintain referential integrity between class and enrollments`() {
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
        assertTrue(enrollments.all { it.classId == classEntity.id })
    }
    
    @Test
    fun `should prevent enrollment in deleted class`() {
        // Given
        val school = createTestSchool()
        val classEntity = createTestClass(school.id)
        classService.deleteClass(classEntity.id)
        
        val studentId = UUID.randomUUID()
        val enrollRequest = EnrollStudentRequest(studentId, classEntity.id)
        
        // When/Then
        assertThrows(Exception::class.java) {
            enrollmentService.enrollStudent(enrollRequest)
        }
    }
    
    @Test
    fun `should handle cascade operations correctly`() {
        // Given
        val school = createTestSchool()
        val class1 = createTestClass(school.id)
        val class2 = createTestClass(school.id)
        
        val student1Id = UUID.randomUUID()
        val student2Id = UUID.randomUUID()
        
        // Enroll students in both classes
        enrollmentService.enrollStudent(EnrollStudentRequest(student1Id, class1.id))
        enrollmentService.enrollStudent(EnrollStudentRequest(student2Id, class1.id))
        enrollmentService.enrollStudent(EnrollStudentRequest(student1Id, class2.id))
        
        // When - delete first class
        classService.deleteClass(class1.id)
        
        // Then - class1 enrollments should be withdrawn
        val class1Enrollments = enrollmentRepository.findByClassIdAndStatus(
            class1.id,
            EnrollmentStatus.ACTIVE
        )
        assertEquals(0, class1Enrollments.size)
        
        // class2 enrollments should remain active
        val class2Enrollments = enrollmentRepository.findByClassIdAndStatus(
            class2.id,
            EnrollmentStatus.ACTIVE
        )
        assertEquals(1, class2Enrollments.size)
        assertEquals(student1Id, class2Enrollments[0].studentId)
    }
    
    @Test
    fun `should track enrollment history per student`() {
        // Given
        val school = createTestSchool()
        val class1 = createTestClass(school.id)
        val class2 = createTestClass(school.id)
        val studentId = UUID.randomUUID()
        
        // Enroll in class1
        val enrollment1 = enrollmentService.enrollStudent(
            EnrollStudentRequest(studentId, class1.id)
        )
        
        // Unenroll from class1
        enrollmentService.unenrollStudent(
            enrollment1.id,
            UnenrollStudentRequest(reason = "Transferred")
        )
        
        // Enroll in class2
        enrollmentService.enrollStudent(EnrollStudentRequest(studentId, class2.id))
        
        // When
        val studentEnrollments = enrollmentRepository.findByStudentId(studentId)
        
        // Then
        assertEquals(2, studentEnrollments.size)
        assertEquals(1, studentEnrollments.count { it.status == EnrollmentStatus.ACTIVE })
        assertEquals(1, studentEnrollments.count { it.status == EnrollmentStatus.WITHDRAWN })
        
        // Verify active enrollment is in class2
        val activeEnrollment = studentEnrollments.first { it.status == EnrollmentStatus.ACTIVE }
        assertEquals(class2.id, activeEnrollment.classId)
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
