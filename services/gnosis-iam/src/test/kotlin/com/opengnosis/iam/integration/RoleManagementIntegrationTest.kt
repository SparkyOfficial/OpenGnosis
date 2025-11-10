package com.opengnosis.iam.integration

import com.opengnosis.domain.Role
import com.opengnosis.iam.dto.AssignRolesRequest
import com.opengnosis.iam.dto.AuthResponse
import com.opengnosis.iam.dto.LoginRequest
import com.opengnosis.iam.dto.RegisterRequest
import com.opengnosis.iam.dto.RoleResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*

class RoleManagementIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var restTemplate: TestRestTemplate
    
    private lateinit var adminToken: String
    private lateinit var testUserId: String
    
    @BeforeEach
    fun setup() {
        // Register admin user
        val adminRegister = RegisterRequest(
            email = "admin@example.com",
            password = "password123",
            firstName = "Admin",
            lastName = "User",
            roles = setOf(Role.ADMINISTRATOR)
        )
        restTemplate.postForEntity("/api/v1/auth/register", adminRegister, Map::class.java)
        
        // Login as admin
        val loginRequest = LoginRequest(email = "admin@example.com", password = "password123")
        val loginResponse = restTemplate.postForEntity(
            "/api/v1/auth/login",
            loginRequest,
            AuthResponse::class.java
        )
        adminToken = loginResponse.body?.accessToken ?: ""
        
        // Register test user
        val userRegister = RegisterRequest(
            email = "roletest@example.com",
            password = "password123",
            firstName = "Role",
            lastName = "Test",
            roles = setOf(Role.STUDENT)
        )
        val userResponse = restTemplate.postForEntity(
            "/api/v1/auth/register",
            userRegister,
            Map::class.java
        )
        testUserId = userResponse.body?.get("userId") as String
    }
    
    @Test
    fun `should get user roles`() {
        val headers = HttpHeaders()
        headers.setBearerAuth(adminToken)
        val entity = HttpEntity<Any>(headers)
        
        val response = restTemplate.exchange(
            "/api/v1/users/$testUserId/roles",
            HttpMethod.GET,
            entity,
            RoleResponse::class.java
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body?.roles?.contains(Role.STUDENT) == true)
    }
    
    @Test
    fun `should assign new roles to user`() {
        val headers = HttpHeaders()
        headers.setBearerAuth(adminToken)
        headers.contentType = MediaType.APPLICATION_JSON
        
        val request = AssignRolesRequest(roles = setOf(Role.TEACHER, Role.STUDENT))
        val entity = HttpEntity(request, headers)
        
        val response = restTemplate.exchange(
            "/api/v1/users/$testUserId/roles",
            HttpMethod.POST,
            entity,
            RoleResponse::class.java
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body?.roles?.contains(Role.TEACHER) == true)
        assertTrue(response.body?.roles?.contains(Role.STUDENT) == true)
    }
}
