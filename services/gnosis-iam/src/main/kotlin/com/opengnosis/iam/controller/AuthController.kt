package com.opengnosis.iam.controller

import com.opengnosis.iam.dto.AuthResponse
import com.opengnosis.iam.dto.LoginRequest
import com.opengnosis.iam.dto.RefreshTokenRequest
import com.opengnosis.iam.dto.RegisterRequest
import com.opengnosis.iam.service.AuthenticationService
import com.opengnosis.iam.service.LogoutService
import com.opengnosis.iam.service.TokenRefreshService
import com.opengnosis.iam.service.UserRegistrationService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val userRegistrationService: UserRegistrationService,
    private val authenticationService: AuthenticationService,
    private val tokenRefreshService: TokenRefreshService,
    private val logoutService: LogoutService
) {
    
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<Map<String, Any>> {
        val user = userRegistrationService.registerUser(request)
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf(
                "message" to "User registered successfully",
                "userId" to user.id.toString(),
                "email" to user.email
            )
        )
    }
    
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AuthResponse> {
        val ipAddress = httpRequest.remoteAddr ?: "unknown"
        val authResponse = authenticationService.authenticate(request, ipAddress)
        
        return ResponseEntity.ok(authResponse)
    }
    
    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        val authResponse = tokenRefreshService.refreshToken(request.refreshToken)
        return ResponseEntity.ok(authResponse)
    }
    
    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization") authorization: String,
        @RequestBody(required = false) refreshToken: String?
    ): ResponseEntity<Map<String, String>> {
        val token = authorization.removePrefix("Bearer ").trim()
        logoutService.logout(token, refreshToken)
        
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }
}
