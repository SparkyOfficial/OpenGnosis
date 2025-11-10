package com.opengnosis.iam.repository

import com.opengnosis.iam.domain.entity.RoleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RoleRepository : JpaRepository<RoleEntity, UUID> {
    fun findByName(name: String): RoleEntity?
    fun findByNameIn(names: Set<String>): Set<RoleEntity>
}
