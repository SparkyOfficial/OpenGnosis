package com.opengnosis.notifier.repository

import com.opengnosis.domain.DeliveryStatus
import com.opengnosis.notifier.entity.NotificationDeliveryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationDeliveryRepository : JpaRepository<NotificationDeliveryEntity, UUID> {
    fun findByUserIdOrderBySentAtDesc(userId: UUID): List<NotificationDeliveryEntity>
    fun findByStatus(status: DeliveryStatus): List<NotificationDeliveryEntity>
}
