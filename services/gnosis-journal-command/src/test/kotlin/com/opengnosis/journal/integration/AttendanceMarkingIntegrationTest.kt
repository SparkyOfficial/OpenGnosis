package com.opengnosis.journal.integration

import com.opengnosis.domain.AttendanceStatus
import com.opengnosis.journal.controller.MarkAttendanceRequest
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
import java.time.LocalDate
import java.util.UUID

class AttendanceMarkingIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var restTemplate: TestRestTemplate
    
    @Autowired
    private lateinit var commandAuditLogRepository: CommandAuditLogRepository
    
    @BeforeEach
    fun setup() {
        commandAuditLogRepository.deleteAll()
    }
    
    @Test
    fun `should accept valid attendance marking command`() {
        // Given
        val request = MarkAttendanceRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            classId = UUID.randomUUID(),
            date = LocalDate.now(),
            lessonNumber = 3,
            status = AttendanceStatus.PRESENT
        )
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/attendance",
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
        assertEquals("MarkAttendanceCommand", auditLog.commandType)
    }
    
    @Test
    fun `should reject attendance marking command for future date`() {
        // Given - date in the future
        val request = MarkAttendanceRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            classId = UUID.randomUUID(),
            date = LocalDate.now().plusDays(1),
            lessonNumber = 2,
            status = AttendanceStatus.PRESENT
        )
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/attendance",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("REJECTED", response.body!!.status)
        assertTrue(response.body!!.message.contains("future date"))
        
        // Verify command was logged as rejected
        val auditLog = commandAuditLogRepository.findById(request.commandId!!).orElse(null)
        assertNotNull(auditLog)
        assertEquals(CommandStatus.REJECTED, auditLog.status)
        assertNotNull(auditLog.errorMessage)
    }
    
    @Test
    fun `should reject attendance marking command with invalid lesson number below minimum`() {
        // Given - lesson number 0 (below minimum of 1)
        val request = MarkAttendanceRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            classId = UUID.randomUUID(),
            date = LocalDate.now(),
            lessonNumber = 0,
            status = AttendanceStatus.PRESENT
        )
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/attendance",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("REJECTED", response.body!!.status)
        assertTrue(response.body!!.message.contains("Invalid lesson number"))
    }
    
    @Test
    fun `should reject attendance marking command with invalid lesson number above maximum`() {
        // Given - lesson number 11 (above maximum of 10)
        val request = MarkAttendanceRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            classId = UUID.randomUUID(),
            date = LocalDate.now(),
            lessonNumber = 11,
            status = AttendanceStatus.ABSENT
        )
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/attendance",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("REJECTED", response.body!!.status)
        assertTrue(response.body!!.message.contains("Invalid lesson number"))
    }
    
    @Test
    fun `should accept attendance marking command with minimum valid lesson number`() {
        // Given - lesson number 1 (minimum valid)
        val request = MarkAttendanceRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            classId = UUID.randomUUID(),
            date = LocalDate.now(),
            lessonNumber = 1,
            status = AttendanceStatus.PRESENT
        )
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/attendance",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("ACCEPTED", response.body!!.status)
    }
    
    @Test
    fun `should accept attendance marking command with maximum valid lesson number`() {
        // Given - lesson number 10 (maximum valid)
        val request = MarkAttendanceRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            classId = UUID.randomUUID(),
            date = LocalDate.now(),
            lessonNumber = 10,
            status = AttendanceStatus.LATE
        )
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/attendance",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("ACCEPTED", response.body!!.status)
    }
    
    @Test
    fun `should enforce idempotency for duplicate attendance marking commands`() {
        // Given
        val commandId = UUID.randomUUID()
        val request = MarkAttendanceRequest(
            commandId = commandId,
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            classId = UUID.randomUUID(),
            date = LocalDate.now(),
            lessonNumber = 4,
            status = AttendanceStatus.ABSENT
        )
        
        // When - submit the same command twice
        val firstResponse: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/attendance",
            request,
            CommandResponse::class.java
        )
        
        val secondResponse: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/attendance",
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
    fun `should accept attendance marking commands with different attendance statuses`() {
        // Test all attendance statuses
        val attendanceStatuses = listOf(
            AttendanceStatus.PRESENT,
            AttendanceStatus.ABSENT,
            AttendanceStatus.LATE,
            AttendanceStatus.EXCUSED
        )
        
        attendanceStatuses.forEach { status ->
            val request = MarkAttendanceRequest(
                commandId = UUID.randomUUID(),
                issuedBy = UUID.randomUUID(),
                studentId = UUID.randomUUID(),
                classId = UUID.randomUUID(),
                date = LocalDate.now(),
                lessonNumber = 5,
                status = status
            )
            
            val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
                "/api/v1/commands/attendance",
                request,
                CommandResponse::class.java
            )
            
            assertEquals(HttpStatus.OK, response.statusCode, "Failed for attendance status: $status")
            assertEquals("ACCEPTED", response.body!!.status)
        }
    }
    
    @Test
    fun `should accept attendance marking command for past date`() {
        // Given - date in the past
        val request = MarkAttendanceRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            classId = UUID.randomUUID(),
            date = LocalDate.now().minusDays(7),
            lessonNumber = 3,
            status = AttendanceStatus.EXCUSED
        )
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/attendance",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("ACCEPTED", response.body!!.status)
    }
}
