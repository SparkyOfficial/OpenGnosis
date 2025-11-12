package com.opengnosis.journal.controller

import com.opengnosis.journal.handler.AssignHomeworkCommandHandler
import com.opengnosis.journal.handler.CommandResponse
import com.opengnosis.journal.handler.MarkAttendanceCommandHandler
import com.opengnosis.journal.handler.PlaceGradeCommandHandler
import com.opengnosis.journal.model.AssignHomeworkCommand
import com.opengnosis.journal.model.MarkAttendanceCommand
import com.opengnosis.journal.model.PlaceGradeCommand
import com.opengnosis.journal.service.CommandAuditService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/commands")
class CommandController(
    private val placeGradeCommandHandler: PlaceGradeCommandHandler,
    private val markAttendanceCommandHandler: MarkAttendanceCommandHandler,
    private val assignHomeworkCommandHandler: AssignHomeworkCommandHandler,
    private val commandAuditService: CommandAuditService
) {
    
    @PostMapping("/grades")
    fun placeGrade(@RequestBody request: PlaceGradeRequest): Mono<ResponseEntity<CommandResponse>> {
        val command = PlaceGradeCommand(
            id = request.commandId ?: UUID.randomUUID(),
            timestamp = Instant.now(),
            issuedBy = request.issuedBy,
            studentId = request.studentId,
            subjectId = request.subjectId,
            gradeValue = request.gradeValue,
            gradeType = request.gradeType,
            comment = request.comment
        )
        
        return placeGradeCommandHandler.handle(command)
            .map { response ->
                when (response.status) {
                    "ACCEPTED" -> ResponseEntity.ok(response)
                    "REJECTED" -> ResponseEntity.badRequest().body(response)
                    else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
                }
            }
    }
    
    @PostMapping("/attendance")
    fun markAttendance(@RequestBody request: MarkAttendanceRequest): Mono<ResponseEntity<CommandResponse>> {
        val command = MarkAttendanceCommand(
            id = request.commandId ?: UUID.randomUUID(),
            timestamp = Instant.now(),
            issuedBy = request.issuedBy,
            studentId = request.studentId,
            classId = request.classId,
            date = request.date,
            lessonNumber = request.lessonNumber,
            status = request.status
        )
        
        return markAttendanceCommandHandler.handle(command)
            .map { response ->
                when (response.status) {
                    "ACCEPTED" -> ResponseEntity.ok(response)
                    "REJECTED" -> ResponseEntity.badRequest().body(response)
                    else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
                }
            }
    }
    
    @PostMapping("/homework")
    fun assignHomework(@RequestBody request: AssignHomeworkRequest): Mono<ResponseEntity<CommandResponse>> {
        val command = AssignHomeworkCommand(
            id = request.commandId ?: UUID.randomUUID(),
            timestamp = Instant.now(),
            issuedBy = request.issuedBy,
            classId = request.classId,
            subjectId = request.subjectId,
            title = request.title,
            description = request.description,
            dueDate = request.dueDate
        )
        
        return assignHomeworkCommandHandler.handle(command)
            .map { response ->
                when (response.status) {
                    "ACCEPTED" -> ResponseEntity.ok(response)
                    "REJECTED" -> ResponseEntity.badRequest().body(response)
                    else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
                }
            }
    }
    
    @GetMapping("/{commandId}/status")
    suspend fun getCommandStatus(@PathVariable commandId: UUID): ResponseEntity<CommandStatusResponse> {
        val auditLog = commandAuditService.getCommandStatus(commandId)
        
        return if (auditLog != null) {
            ResponseEntity.ok(
                CommandStatusResponse(
                    commandId = auditLog.id,
                    commandType = auditLog.commandType,
                    status = auditLog.status.name,
                    issuedBy = auditLog.issuedBy,
                    timestamp = auditLog.timestamp,
                    processedAt = auditLog.processedAt,
                    errorMessage = auditLog.errorMessage
                )
            )
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
