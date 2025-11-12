package com.opengnosis.journal.repository

import com.opengnosis.journal.entity.CommandAuditLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CommandAuditLogRepository : JpaRepository<CommandAuditLog, UUID>
