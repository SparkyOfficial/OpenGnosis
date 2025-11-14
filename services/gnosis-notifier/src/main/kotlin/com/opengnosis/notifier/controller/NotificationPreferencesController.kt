package com.opengnosis.notifier.controller

import com.opengnosis.domain.NotificationType
import com.opengnosis.notifier.entity.NotificationPreferencesEntity
import com.opengnosis.notifier.service.NotificationPreferencesService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notifications/preferences")
class NotificationPreferencesController(
    private val preferencesService: NotificationPreferencesService
) {
    
    @GetMapping("/{userId}")
    fun getPreferences(@PathVariable userId: UUID): ResponseEntity<NotificationPreferencesResponse> {
        val preferences = preferencesService.getPreferences(userId)
        return ResponseEntity.ok(preferences.toResponse())
    }
    
    @PutMapping("/{userId}")
    fun updatePreferences(
        @PathVariable userId: UUID,
        @RequestBody request: UpdatePreferencesRequest
    ): ResponseEntity<NotificationPreferencesResponse> {
        val updated = preferencesService.updatePreferences(
            userId = userId,
            emailEnabled = request.emailEnabled,
            pushEnabled = request.pushEnabled,
            smsEnabled = request.smsEnabled,
            notificationTypes = request.notificationTypes
        )
        return ResponseEntity.ok(updated.toResponse())
    }
}

data class NotificationPreferencesResponse(
    val userId: UUID,
    val emailEnabled: Boolean,
    val pushEnabled: Boolean,
    val smsEnabled: Boolean,
    val notificationTypes: Set<NotificationType>
)

data class UpdatePreferencesRequest(
    val emailEnabled: Boolean? = null,
    val pushEnabled: Boolean? = null,
    val smsEnabled: Boolean? = null,
    val notificationTypes: Set<NotificationType>? = null
)

private fun NotificationPreferencesEntity.toResponse() = NotificationPreferencesResponse(
    userId = userId,
    emailEnabled = emailEnabled,
    pushEnabled = pushEnabled,
    smsEnabled = smsEnabled,
    notificationTypes = notificationTypes
)
