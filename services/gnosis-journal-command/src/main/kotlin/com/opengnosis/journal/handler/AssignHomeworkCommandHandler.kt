package com.opengnosis.journal.handler

import com.opengnosis.events.HomeworkAssignedEvent
import com.opengnosis.journal.entity.CommandStatus
import com.opengnosis.journal.model.AssignHomeworkCommand
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
class AssignHomeworkCommandHandler(
    private val validationService: ValidationService,
    private val commandAuditService: CommandAuditService,
    private val eventPublisher: ReactiveEventPublisher,
    @Value("\${kafka.topics.homework-assigned}") private val homeworkAssignedTopic: String
) {
    private val logger = LoggerFactory.getLogger(AssignHomeworkCommandHandler::class.java)
    
    fun handle(command: AssignHomeworkCommand): Mono<CommandResponse> = mono {
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
            
            // Validate due date (not in past)
            if (command.dueDate.isBefore(LocalDate.now())) {
                val errorMsg = "Due date cannot be in the past: ${command.dueDate}"
                logger.warn("Validation failed for command {}: {}", command.id, errorMsg)
                commandAuditService.logCommand(command, CommandStatus.REJECTED, errorMsg)
                return@mono CommandResponse(
                    commandId = command.id,
                    status = "REJECTED",
                    message = errorMsg
                )
            }
            
            // Validate title and description
            if (command.title.isBlank()) {
                val errorMsg = "Homework title cannot be blank"
                logger.warn("Validation failed for command {}: {}", command.id, errorMsg)
                commandAuditService.logCommand(command, CommandStatus.REJECTED, errorMsg)
                return@mono CommandResponse(
                    commandId = command.id,
                    status = "REJECTED",
                    message = errorMsg
                )
            }
            
            if (command.description.isBlank()) {
                val errorMsg = "Homework description cannot be blank"
                logger.warn("Validation failed for command {}: {}", command.id, errorMsg)
                commandAuditService.logCommand(command, CommandStatus.REJECTED, errorMsg)
                return@mono CommandResponse(
                    commandId = command.id,
                    status = "REJECTED",
                    message = errorMsg
                )
            }
            
            // Validate teacher-class-subject association
            if (!validationService.validateTeacherClassSubjectAssociation(
                    command.issuedBy,
                    command.classId,
                    command.subjectId
                )) {
                val errorMsg = "Teacher ${command.issuedBy} is not authorized to assign homework for class ${command.classId} and subject ${command.subjectId}"
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
            val event = HomeworkAssignedEvent(
                aggregateId = command.classId,
                classId = command.classId,
                subjectId = command.subjectId,
                title = command.title,
                description = command.description,
                dueDate = command.dueDate,
                assignedBy = command.issuedBy
            )
            
            // Publish event asynchronously (fire and forget)
            eventPublisher.publishEvent(homeworkAssignedTopic, event).subscribe()
            
            logger.info("Command {} accepted and event published", command.id)
            CommandResponse(
                commandId = command.id,
                status = "ACCEPTED",
                message = "Homework assignment command accepted"
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
