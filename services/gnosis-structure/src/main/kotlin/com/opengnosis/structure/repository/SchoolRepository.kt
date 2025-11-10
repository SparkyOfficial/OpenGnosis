package com.opengnosis.structure.repository

import com.opengnosis.domain.SchoolStatus
import com.opengnosis.structure.domain.entity.SchoolEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SchoolRepository : JpaRepository<SchoolEntity, UUID> {
    fun findByStatus(status: SchoolStatus): List<SchoolEntity>
    fun findByNameContainingIgnoreCase(name: String): List<SchoolEntity>
}
