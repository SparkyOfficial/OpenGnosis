package com.opengnosis.iam.repository

import com.opengnosis.iam.domain.entity.PermissionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PermissionRepository : JpaRepository<PermissionEntity, UUID> {
    fun findByName(name: String): PermissionEntity?
    fun findByResourceAndAction(resource: String, action: String): PermissionEntity?
}
