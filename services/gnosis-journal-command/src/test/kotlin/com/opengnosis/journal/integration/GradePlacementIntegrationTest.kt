package com.opengnosis.journal.integration

import com.opengnosis.domain.GradeType
import com.opengnosis.journal.controller.PlaceGradeRequest
import com.opengnosis.journal.entity.CommandStatus
import com.opengnosis.journal.handler.CommandResponse
import com.opengnosis.journal.repository.CommandAuditLogRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.UUID

class GradePlacementIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var restTemplate: TestRestTemplate
    
    @Autowired
    private lateinit var commandAuditLogRepository: CommandAuditLogRepository
    
    @BeforeEach
    fun setup() {
        commandAuditLogRepository.deleteAll()
    }
    
    @Test
    fun `should accept valid grade placement command`() {
        // Given
        val request = PlaceGradeRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            subjectId = UUID.randomUUID(),
            gradeValue = 8,
            gradeType = GradeType.EXAM,
            comment = "Good performance"
        )
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/grades",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("ACCEPTED", response.body!!.status)
        assertEquals(request.commandId, response.body!!.commandId)
        assertTrue(response.body!!.message.contains("accepted"))
        
        // Verify command was logged in audit log
        val auditLog = commandAuditLogRepository.findById(request.commandId!!).orElse(null)
        assertNotNull(auditLog)
        assertEquals(CommandStatus.ACCEPTED, auditLog.status)
        assertEquals(request.issuedBy, auditLog.issuedBy)
        assertEquals("PlaceGradeCommand", auditLog.commandType)
    }
    
    @Test
    fun `should reject grade placement command with invalid grade value`() {
        // Given - grade value outside valid range (1-10)
        val request = PlaceGradeRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            subjectId = UUID.randomUUID(),
            gradeValue = 15,
            gradeType = GradeType.EXAM,
            comment = null
        )
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/grades",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("REJECTED", response.body!!.status)
        assertTrue(response.body!!.message.contains("Invalid grade value"))
        
        // Verify command was logged as rejected
        val auditLog = commandAuditLogRepository.findById(request.commandId!!).orElse(null)
        assertNotNull(auditLog)
        assertEquals(CommandStatus.REJECTED, auditLog.status)
        assertNotNull(auditLog.errorMessage)
        assertTrue(auditLog.errorMessage!!.contains("Invalid grade value"))
    }
    
    @Test
    fun `should reject grade placement command with grade value below minimum`() {
        // Given - grade value 0 (below minimum of 1)
        val request = PlaceGradeRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            subjectId = UUID.randomUUID(),
            gradeValue = 0,
            gradeType = GradeType.QUIZ,
            comment = null
        )
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/grades",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("REJECTED", response.body!!.status)
    }
    
    @Test
    fun `should accept grade placement command with minimum valid grade`() {
        // Given - grade value 1 (minimum valid)
        val request = PlaceGradeRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            subjectId = UUID.randomUUID(),
            gradeValue = 1,
            gradeType = GradeType.HOMEWORK,
            comment = "Needs improvement"
        )
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/grades",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("ACCEPTED", response.body!!.status)
    }
    
    @Test
    fun `should accept grade placement command with maximum valid grade`() {
        // Given - grade value 10 (maximum valid)
        val request = PlaceGradeRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            subjectId = UUID.randomUUID(),
            gradeValue = 10,
            gradeType = GradeType.FINAL,
            comment = "Excellent work"
        )
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/grades",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("ACCEPTED", response.body!!.status)
    }
    
    @Test
    fun `should enforce idempotency for duplicate grade placement commands`() {
        // Given
        val commandId = UUID.randomUUID()
        val request = PlaceGradeRequest(
            commandId = commandId,
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            subjectId = UUID.randomUUID(),
            gradeValue = 7,
            gradeType = GradeType.CLASSWORK,
            comment = "Good work"
        )
        
        // When - submit the same command twice
        val firstResponse: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/grades",
            request,
            CommandResponse::class.java
        )
        
        val secondResponse: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/grades",
            request,
            CommandResponse::class.java
        )
        
        // Then - both should succeed but second should indicate already processed
        assertEquals(HttpStatus.OK, firstResponse.statusCode)
        assertEquals("ACCEPTED", firstResponse.body!!.status)
        
        assertEquals(HttpStatus.OK, secondResponse.statusCode)
        assertEquals("ACCEPTED", secondResponse.body!!.status)
        assertTrue(secondResponse.body!!.message.contains("already processed"))
        
        // Verify only one entry in audit log
        val auditLogs = commandAuditLogRepository.findAll()
        val commandLogs = auditLogs.filter { it.id == commandId }
        assertEquals(1, commandLogs.size)
    }
    
    @Test
    fun `should accept grade placement commands with different grade types`() {
        // Test all grade types
        val gradeTypes = listOf(
            GradeType.EXAM,
            GradeType.QUIZ,
            GradeType.HOMEWORK,
            GradeType.CLASSWORK,
            GradeType.FINAL
        )
        
        gradeTypes.forEach { gradeType ->
            val request = PlaceGradeRequest(
                commandId = UUID.randomUUID(),
                issuedBy = UUID.randomUUID(),
                studentId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                gradeValue = 8,
                gradeType = gradeType,
                comment = "Test for $gradeType"
            )
            
            val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
                "/api/v1/commands/grades",
                request,
                CommandResponse::class.java
            )
            
            assertEquals(HttpStatus.OK, response.statusCode, "Failed for grade type: $gradeType")
            assertEquals("ACCEPTED", response.body!!.status)
        }
    }
}
