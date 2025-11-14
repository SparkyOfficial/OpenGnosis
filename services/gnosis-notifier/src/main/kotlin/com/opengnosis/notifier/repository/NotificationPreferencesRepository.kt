package com.opengnosis.notifier.repository

import com.opengnosis.notifier.entity.NotificationPreferencesEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationPreferencesRepository : JpaRepository<NotificationPreferencesEntity, UUID>
