package com.opengnosis.notifier.controller

import com.opengnosis.notifier.entity.DeviceTokenEntity
import com.opengnosis.notifier.service.PushNotificationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notifications/devices")
class DeviceTokenController(
    private val pushNotificationService: PushNotificationService
) {
    
    @PostMapping("/register")
    fun registerDevice(@RequestBody request: RegisterDeviceRequest): ResponseEntity<DeviceTokenResponse> {
        val token = pushNotificationService.registerDeviceToken(
            userId = request.userId,
            token = request.token,
            deviceType = request.deviceType
        )
        return ResponseEntity.ok(token.toResponse())
    }
    
    @DeleteMapping("/deactivate")
    fun deactivateDevice(@RequestBody request: DeactivateDeviceRequest): ResponseEntity<Void> {
        pushNotificationService.deactivateDeviceToken(request.token)
        return ResponseEntity.noContent().build()
    }
}

data class RegisterDeviceRequest(
    val userId: UUID,
    val token: String,
    val deviceType: String
)

data class DeactivateDeviceRequest(
    val token: String
)

data class DeviceTokenResponse(
    val id: UUID,
    val userId: UUID,
    val deviceType: String,
    val active: Boolean
)

private fun DeviceTokenEntity.toResponse() = DeviceTokenResponse(
    id = id,
    userId = userId,
    deviceType = deviceType,
    active = active
)
