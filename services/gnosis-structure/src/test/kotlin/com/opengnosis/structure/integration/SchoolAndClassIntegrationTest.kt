package com.opengnosis.structure.integration

import com.opengnosis.domain.SchoolStatus
import com.opengnosis.structure.domain.entity.ClassStatus
import com.opengnosis.structure.dto.CreateClassRequest
import com.opengnosis.structure.dto.CreateSchoolRequest
import com.opengnosis.structure.repository.ClassRepository
import com.opengnosis.structure.repository.SchoolRepository
import com.opengnosis.structure.service.ClassService
import com.opengnosis.structure.service.SchoolService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class SchoolAndClassIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var schoolService: SchoolService
    
    @Autowired
    private lateinit var classService: ClassService
    
    @Autowired
    private lateinit var schoolRepository: SchoolRepository
    
    @Autowired
    private lateinit var classRepository: ClassRepository
    
    @Test
    fun `should create school successfully`() {
        // Given
        val principalId = UUID.randomUUID()
        val request = CreateSchoolRequest(
            name = "Test High School",
            address = "123 Education Street, Test City",
            principalId = principalId
        )
        
        // When
        val response = schoolService.createSchool(request)
        
        // Then
        assertNotNull(response.id)
        assertEquals("Test High School", response.name)
        assertEquals("123 Education Street, Test City", response.address)
        assertEquals(principalId, response.principalId)
        assertEquals(SchoolStatus.ACTIVE, response.status)
        
        // Verify in database
        val savedSchool = schoolRepository.findById(response.id)
        assertTrue(savedSchool.isPresent)
        assertEquals("Test High School", savedSchool.get().name)
    }
    
    @Test
    fun `should retrieve school by id`() {
        // Given
        val principalId = UUID.randomUUID()
        val createRequest = CreateSchoolRequest(
            name = "Test School",
            address = "456 Test Avenue",
            principalId = principalId
        )
        val created = schoolService.createSchool(createRequest)
        
        // When
        val retrieved = schoolService.getSchool(created.id)
        
        // Then
        assertEquals(created.id, retrieved.id)
        assertEquals(created.name, retrieved.name)
        assertEquals(created.address, retrieved.address)
    }
    
    @Test
    fun `should create class successfully`() {
        // Given
        val principalId = UUID.randomUUID()
        val schoolRequest = CreateSchoolRequest(
            name = "Test School",
            address = "789 School Road",
            principalId = principalId
        )
        val school = schoolService.createSchool(schoolRequest)
        
        val academicYearId = UUID.randomUUID()
        val classTeacherId = UUID.randomUUID()
        val classRequest = CreateClassRequest(
            schoolId = school.id,
            academicYearId = academicYearId,
            name = "Grade 10A",
            grade = 10,
            classTeacherId = classTeacherId,
            capacity = 30
        )
        
        // When
        val classResponse = classService.createClass(classRequest)
        
        // Then
        assertNotNull(classResponse.id)
        assertEquals(school.id, classResponse.schoolId)
        assertEquals(academicYearId, classResponse.academicYearId)
        assertEquals("Grade 10A", classResponse.name)
        assertEquals(10, classResponse.grade)
        assertEquals(classTeacherId, classResponse.classTeacherId)
        assertEquals(30, classResponse.capacity)
        assertEquals(ClassStatus.ACTIVE, classResponse.status)
        
        // Verify in database
        val savedClass = classRepository.findById(classResponse.id)
        assertTrue(savedClass.isPresent)
        assertEquals("Grade 10A", savedClass.get().name)
    }
    
    @Test
    fun `should retrieve classes by school id`() {
        // Given
        val principalId = UUID.randomUUID()
        val schoolRequest = CreateSchoolRequest(
            name = "Multi-Class School",
            address = "100 Education Blvd",
            principalId = principalId
        )
        val school = schoolService.createSchool(schoolRequest)
        
        val academicYearId = UUID.randomUUID()
        val teacherId1 = UUID.randomUUID()
        val teacherId2 = UUID.randomUUID()
        
        classService.createClass(CreateClassRequest(
            schoolId = school.id,
            academicYearId = academicYearId,
            name = "Grade 9A",
            grade = 9,
            classTeacherId = teacherId1
        ))
        
        classService.createClass(CreateClassRequest(
            schoolId = school.id,
            academicYearId = academicYearId,
            name = "Grade 9B",
            grade = 9,
            classTeacherId = teacherId2
        ))
        
        // When
        val classes = classRepository.findBySchoolId(school.id)
        
        // Then
        assertEquals(2, classes.size)
        assertTrue(classes.any { it.name == "Grade 9A" })
        assertTrue(classes.any { it.name == "Grade 9B" })
    }
    
    @Test
    fun `should enforce unique class name per school and academic year`() {
        // Given
        val principalId = UUID.randomUUID()
        val schoolRequest = CreateSchoolRequest(
            name = "Unique Class School",
            address = "200 Unique Street",
            principalId = principalId
        )
        val school = schoolService.createSchool(schoolRequest)
        
        val academicYearId = UUID.randomUUID()
        val teacherId = UUID.randomUUID()
        
        val classRequest = CreateClassRequest(
            schoolId = school.id,
            academicYearId = academicYearId,
            name = "Grade 10A",
            grade = 10,
            classTeacherId = teacherId
        )
        
        // When
        classService.createClass(classRequest)
        
        // Then - attempting to create duplicate should fail
        assertThrows(Exception::class.java) {
            classService.createClass(classRequest)
        }
    }
}
