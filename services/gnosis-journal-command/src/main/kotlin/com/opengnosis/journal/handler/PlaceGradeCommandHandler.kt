package com.opengnosis.journal.handler

import com.opengnosis.events.GradePlacedEvent
import com.opengnosis.journal.entity.CommandStatus
import com.opengnosis.journal.model.PlaceGradeCommand
import com.opengnosis.journal.service.CommandAuditService
import com.opengnosis.journal.service.ReactiveEventPublisher
import com.opengnosis.journal.service.ValidationService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class PlaceGradeCommandHandler(
    private val validationService: ValidationService,
    private val commandAuditService: CommandAuditService,
    private val eventPublisher: ReactiveEventPublisher,
    @Value("\${kafka.topics.grade-placed}") private val gradePlacedTopic: String
) {
    private val logger = LoggerFactory.getLogger(PlaceGradeCommandHandler::class.java)
    
    fun handle(command: PlaceGradeCommand): Mono<CommandResponse> = mono {
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
            
            // Validate grade value
            if (!validationService.validateGradeValue(command.gradeValue)) {
                val errorMsg = "Invalid grade value: ${command.gradeValue}. Must be between 1 and 10"
                logger.warn("Validation failed for command {}: {}", command.id, errorMsg)
                commandAuditService.logCommand(command, CommandStatus.REJECTED, errorMsg)
                return@mono CommandResponse(
                    commandId = command.id,
                    status = "REJECTED",
                    message = errorMsg
                )
            }
            
            // Validate student-subject association
            if (!validationService.validateStudentSubjectAssociation(command.studentId, command.subjectId)) {
                val errorMsg = "Student ${command.studentId} is not enrolled in subject ${command.subjectId}"
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
            val event = GradePlacedEvent(
                aggregateId = command.studentId,
                studentId = command.studentId,
                subjectId = command.subjectId,
                gradeValue = command.gradeValue,
                gradeType = command.gradeType,
                comment = command.comment,
                placedBy = command.issuedBy
            )
            
            // Publish event asynchronously (fire and forget)
            eventPublisher.publishEvent(gradePlacedTopic, event).subscribe()
            
            logger.info("Command {} accepted and event published", command.id)
            CommandResponse(
                commandId = command.id,
                status = "ACCEPTED",
                message = "Grade placement command accepted"
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

data class CommandResponse(
    val commandId: java.util.UUID,
    val status: String,
    val message: String
)
