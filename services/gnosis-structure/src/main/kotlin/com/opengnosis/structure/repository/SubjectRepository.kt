package com.opengnosis.structure.repository

import com.opengnosis.structure.domain.entity.SubjectEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SubjectRepository : JpaRepository<SubjectEntity, UUID> {
    fun findByCode(code: String): SubjectEntity?
    fun findByNameContainingIgnoreCase(name: String): List<SubjectEntity>
}
