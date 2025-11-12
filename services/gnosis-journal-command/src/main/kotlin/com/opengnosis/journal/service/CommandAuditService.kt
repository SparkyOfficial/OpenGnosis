package com.opengnosis.journal.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.opengnosis.journal.entity.CommandAuditLog
import com.opengnosis.journal.entity.CommandStatus
import com.opengnosis.journal.model.JournalCommand
import com.opengnosis.journal.repository.CommandAuditLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CommandAuditService(
    private val commandAuditLogRepository: CommandAuditLogRepository,
    private val objectMapper: ObjectMapper
) {
    
    suspend fun isCommandProcessed(commandId: UUID): Boolean = withContext(Dispatchers.IO) {
        commandAuditLogRepository.existsById(commandId)
    }
    
    suspend fun logCommand(
        command: JournalCommand,
        status: CommandStatus,
        errorMessage: String? = null
    ): CommandAuditLog = withContext(Dispatchers.IO) {
        val auditLog = CommandAuditLog(
            id = command.id,
            commandType = command::class.simpleName ?: "Unknown",
            issuedBy = command.issuedBy,
            timestamp = command.timestamp,
            status = status,
            payload = objectMapper.writeValueAsString(command),
            errorMessage = errorMessage
        )
        commandAuditLogRepository.save(auditLog)
    }
    
    suspend fun getCommandStatus(commandId: UUID): CommandAuditLog? = withContext(Dispatchers.IO) {
        commandAuditLogRepository.findById(commandId).orElse(null)
    }
}
