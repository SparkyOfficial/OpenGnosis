package com.opengnosis.notifier.repository

import com.opengnosis.notifier.entity.DeviceTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DeviceTokenRepository : JpaRepository<DeviceTokenEntity, UUID> {
    fun findByUserIdAndActiveTrue(userId: UUID): List<DeviceTokenEntity>
    fun findByToken(token: String): DeviceTokenEntity?
}
