package com.opengnosis.analytics.integration

import com.opengnosis.analytics.document.StudentSearchDocument
import com.opengnosis.analytics.repository.StudentSearchRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import java.util.UUID

class ElasticsearchSearchIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var studentSearchRepository: StudentSearchRepository
    
    @Autowired
    private lateinit var elasticsearchOperations: ElasticsearchOperations
    
    @BeforeEach
    fun setup() {
        studentSearchRepository.deleteAll()
        
        // Wait for Elasticsearch to process deletions
        Thread.sleep(1000)
    }
    
    @Test
    fun `should index and search student by name`() {
        // Given
        val studentId = UUID.randomUUID()
        val document = StudentSearchDocument(
            id = studentId.toString(),
            studentId = studentId.toString(),
            firstName = "John",
            lastName = "Doe",
            fullName = "John Doe",
            email = "john.doe@example.com",
            classIds = listOf(UUID.randomUUID().toString())
        )
        
        studentSearchRepository.save(document)
        
        // Wait for Elasticsearch to index
        Thread.sleep(2000)
        
        // When
        val results = studentSearchRepository.searchStudents("John")
        
        // Then
        assertTrue(results.isNotEmpty())
        val foundStudent = results.find { it.studentId == studentId.toString() }
        assertNotNull(foundStudent)
        assertEquals("John", foundStudent!!.firstName)
        assertEquals("Doe", foundStudent.lastName)
    }
    
    @Test
    fun `should search student by email`() {
        // Given
        val studentId = UUID.randomUUID()
        val email = "jane.smith@example.com"
        val document = StudentSearchDocument(
            id = studentId.toString(),
            studentId = studentId.toString(),
            firstName = "Jane",
            lastName = "Smith",
            fullName = "Jane Smith",
            email = email,
            classIds = listOf(UUID.randomUUID().toString())
        )
        
        studentSearchRepository.save(document)
        
        // Wait for Elasticsearch to index
        Thread.sleep(2000)
        
        // When
        val result = studentSearchRepository.findByEmail(email)
        
        // Then
        assertNotNull(result)
        assertEquals(email, result!!.email)
        assertEquals("Jane", result.firstName)
        assertEquals("Smith", result.lastName)
    }
    
    @Test
    fun `should search student by student ID`() {
        // Given
        val studentId = UUID.randomUUID()
        val document = StudentSearchDocument(
            id = studentId.toString(),
            studentId = studentId.toString(),
            firstName = "Alice",
            lastName = "Johnson",
            fullName = "Alice Johnson",
            email = "alice.johnson@example.com",
            classIds = listOf(UUID.randomUUID().toString())
        )
        
        studentSearchRepository.save(document)
        
        // Wait for Elasticsearch to index
        Thread.sleep(2000)
        
        // When
        val result = studentSearchRepository.findByStudentId(studentId.toString())
        
        // Then
        assertNotNull(result)
        assertEquals(studentId.toString(), result!!.studentId)
        assertEquals("Alice", result.firstName)
    }
    
    @Test
    fun `should perform fuzzy search for student names`() {
        // Given
        val students = listOf(
            StudentSearchDocument(
                id = UUID.randomUUID().toString(),
                studentId = UUID.randomUUID().toString(),
                firstName = "Michael",
                lastName = "Anderson",
                fullName = "Michael Anderson",
                email = "michael.anderson@example.com",
                classIds = listOf(UUID.randomUUID().toString())
            ),
            StudentSearchDocument(
                id = UUID.randomUUID().toString(),
                studentId = UUID.randomUUID().toString(),
                firstName = "Michelle",
                lastName = "Andrews",
                fullName = "Michelle Andrews",
                email = "michelle.andrews@example.com",
                classIds = listOf(UUID.randomUUID().toString())
            )
        )
        
        studentSearchRepository.saveAll(students)
        
        // Wait for Elasticsearch to index
        Thread.sleep(2000)
        
        // When - search with slight misspelling
        val results = studentSearchRepository.searchStudents("Micheal")
        
        // Then - should still find Michael due to fuzzy matching
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.firstName == "Michael" || it.firstName == "Michelle" })
    }
    
    @Test
    fun `should search across multiple fields`() {
        // Given
        val studentId = UUID.randomUUID()
        val document = StudentSearchDocument(
            id = studentId.toString(),
            studentId = studentId.toString(),
            firstName = "Robert",
            lastName = "Williams",
            fullName = "Robert Williams",
            email = "robert.williams@example.com",
            classIds = listOf(UUID.randomUUID().toString())
        )
        
        studentSearchRepository.save(document)
        
        // Wait for Elasticsearch to index
        Thread.sleep(2000)
        
        // When - search by last name
        val resultsByLastName = studentSearchRepository.searchStudents("Williams")
        
        // When - search by email part
        val resultsByEmail = studentSearchRepository.searchStudents("robert.williams")
        
        // Then
        assertTrue(resultsByLastName.isNotEmpty())
        assertTrue(resultsByEmail.isNotEmpty())
        
        val foundByLastName = resultsByLastName.find { it.studentId == studentId.toString() }
        val foundByEmail = resultsByEmail.find { it.studentId == studentId.toString() }
        
        assertNotNull(foundByLastName)
        assertNotNull(foundByEmail)
    }
    
    @Test
    fun `should return empty results for non-existent student`() {
        // Given - no students indexed
        
        // When
        val results = studentSearchRepository.searchStudents("NonExistentStudent")
        
        // Then
        assertTrue(results.isEmpty())
    }
    
    @Test
    fun `should index multiple students and search efficiently`() {
        // Given
        val students = (1..10).map { i ->
            StudentSearchDocument(
                id = UUID.randomUUID().toString(),
                studentId = UUID.randomUUID().toString(),
                firstName = "Student$i",
                lastName = "Test$i",
                fullName = "Student$i Test$i",
                email = "student$i@example.com",
                classIds = listOf(UUID.randomUUID().toString())
            )
        }
        
        studentSearchRepository.saveAll(students)
        
        // Wait for Elasticsearch to index
        Thread.sleep(2000)
        
        // When
        val results = studentSearchRepository.searchStudents("Student")
        
        // Then
        assertTrue(results.size >= 10)
    }
    
    @Test
    fun `should handle special characters in search`() {
        // Given
        val studentId = UUID.randomUUID()
        val document = StudentSearchDocument(
            id = studentId.toString(),
            studentId = studentId.toString(),
            firstName = "María",
            lastName = "García",
            fullName = "María García",
            email = "maria.garcia@example.com",
            classIds = listOf(UUID.randomUUID().toString())
        )
        
        studentSearchRepository.save(document)
        
        // Wait for Elasticsearch to index
        Thread.sleep(2000)
        
        // When
        val results = studentSearchRepository.searchStudents("Maria")
        
        // Then
        assertTrue(results.isNotEmpty())
        val foundStudent = results.find { it.email == "maria.garcia@example.com" }
        assertNotNull(foundStudent)
    }
}
