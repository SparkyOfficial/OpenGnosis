package com.opengnosis.iam.integration

import com.opengnosis.domain.Role
import com.opengnosis.iam.dto.AuthResponse
import com.opengnosis.iam.dto.LoginRequest
import com.opengnosis.iam.dto.RefreshTokenRequest
import com.opengnosis.iam.dto.RegisterRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus

class AuthenticationIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var restTemplate: TestRestTemplate
    
    @BeforeEach
    fun setup() {
        // Register a test user
        val registerRequest = RegisterRequest(
            email = "auth-test@example.com",
            password = "password123",
            firstName = "Auth",
            lastName = "Test",
            roles = setOf(Role.STUDENT)
        )
        restTemplate.postForEntity("/api/v1/auth/register", registerRequest, Map::class.java)
    }
    
    @Test
    fun `should authenticate user and return JWT token`() {
        val loginRequest = LoginRequest(
            email = "auth-test@example.com",
            password = "password123"
        )
        
        val response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            loginRequest,
            AuthResponse::class.java
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertNotNull(response.body?.accessToken)
        assertNotNull(response.body?.refreshToken)
        assertEquals("Bearer", response.body?.tokenType)
        assertTrue(response.body?.roles?.contains(Role.STUDENT) == true)
    }
    
    @Test
    fun `should reject invalid credentials`() {
        val loginRequest = LoginRequest(
            email = "auth-test@example.com",
            password = "wrongpassword"
        )
        
        val response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            loginRequest,
            Map::class.java
        )
        
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }
    
    @Test
    fun `should refresh token successfully`() {
        // First login
        val loginRequest = LoginRequest(
            email = "auth-test@example.com",
            password = "password123"
        )
        
        val loginResponse = restTemplate.postForEntity(
            "/api/v1/auth/login",
            loginRequest,
            AuthResponse::class.java
        )
        
        val refreshToken = loginResponse.body?.refreshToken
        assertNotNull(refreshToken)
        
        // Refresh token
        val refreshRequest = RefreshTokenRequest(refreshToken = refreshToken!!)
        val refreshResponse = restTemplate.postForEntity(
            "/api/v1/auth/refresh",
            refreshRequest,
            AuthResponse::class.java
        )
        
        assertEquals(HttpStatus.OK, refreshResponse.statusCode)
        assertNotNull(refreshResponse.body?.accessToken)
        assertNotEquals(refreshToken, refreshResponse.body?.refreshToken)
    }
}
