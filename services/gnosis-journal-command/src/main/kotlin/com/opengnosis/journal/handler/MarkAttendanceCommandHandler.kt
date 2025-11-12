package com.opengnosis.journal.handler

import com.opengnosis.events.AttendanceMarkedEvent
import com.opengnosis.journal.entity.CommandStatus
import com.opengnosis.journal.model.MarkAttendanceCommand
import com.opengnosis.journal.service.CommandAuditService
import com.opengnosis.journal.service.ReactiveEventPublisher
import com.opengnosis.journal.service.ValidationService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.LocalDate

@Component
class MarkAttendanceCommandHandler(
    private val validationService: ValidationService,
    private val commandAuditService: CommandAuditService,
    private val eventPublisher: ReactiveEventPublisher,
    @Value("\${kafka.topics.attendance-marked}") private val attendanceMarkedTopic: String
) {
    private val logger = LoggerFactory.getLogger(MarkAttendanceCommandHandler::class.java)
    
    fun handle(command: MarkAttendanceCommand): Mono<CommandResponse> = mono {
        try {
            // Check idempotency
            if (commandAuditService.isCommandProcessed(command.id)) {
                logger.info("Command {} already processed, returning success", command.id)
                return@mono CommandResponse(
                    commandId = command.id,
                    status = "ACCEPTED",
                    message = "Command already processed"
                )
            }
            
            // Validate date range (not in future)
            if (command.date.isAfter(LocalDate.now())) {
                val errorMsg = "Cannot mark attendance for future date: ${command.date}"
                logger.warn("Validation failed for command {}: {}", command.id, errorMsg)
                commandAuditService.logCommand(command, CommandStatus.REJECTED, errorMsg)
                return@mono CommandResponse(
                    commandId = command.id,
                    status = "REJECTED",
                    message = errorMsg
                )
            }
            
            // Validate lesson number
            if (command.lessonNumber < 1 || command.lessonNumber > 10) {
                val errorMsg = "Invalid lesson number: ${command.lessonNumber}. Must be between 1 and 10"
                logger.warn("Validation failed for command {}: {}", command.id, errorMsg)
                commandAuditService.logCommand(command, CommandStatus.REJECTED, errorMsg)
                return@mono CommandResponse(
                    commandId = command.id,
                    status = "REJECTED",
                    message = errorMsg
                )
            }
            
            // Validate student enrollment
            if (!validationService.validateStudentEnrollment(command.studentId, command.classId)) {
                val errorMsg = "Student ${command.studentId} is not enrolled in class ${command.classId}"
                logger.warn("Validation failed for command {}: {}", command.id, errorMsg)
                commandAuditService.logCommand(command, CommandStatus.REJECTED, errorMsg)
                return@mono CommandResponse(
                    commandId = command.id,
                    status = "REJECTED",
                    message = errorMsg
                )
            }
            
            // Log command as accepted
            commandAuditService.logCommand(command, CommandStatus.ACCEPTED)
            
            // Create and publish event
            val event = AttendanceMarkedEvent(
                aggregateId = command.studentId,
                studentId = command.studentId,
                classId = command.classId,
                date = command.date,
                lessonNumber = command.lessonNumber,
                status = command.status,
                markedBy = command.issuedBy
            )
            
            // Publish event asynchronously (fire and forget)
            eventPublisher.publishEvent(attendanceMarkedTopic, event).subscribe()
            
            logger.info("Command {} accepted and event published", command.id)
            CommandResponse(
                commandId = command.id,
                status = "ACCEPTED",
                message = "Attendance marking command accepted"
            )
        } catch (e: Exception) {
            logger.error("Error processing command {}", command.id, e)
            commandAuditService.logCommand(command, CommandStatus.FAILED, e.message)
            CommandResponse(
                commandId = command.id,
                status = "FAILED",
                message = "Internal error: ${e.message}"
            )
        }
    }
}
