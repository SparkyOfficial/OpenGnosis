package com.opengnosis.iam.integration

import com.opengnosis.domain.Role
import com.opengnosis.iam.dto.RegisterRequest
import com.opengnosis.iam.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus

class UserRegistrationIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var restTemplate: TestRestTemplate
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Test
    fun `should register user successfully`() {
        val request = RegisterRequest(
            email = "test@example.com",
            password = "password123",
            firstName = "John",
            lastName = "Doe",
            roles = setOf(Role.STUDENT)
        )
        
        val response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            request,
            Map::class.java
        )
        
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.containsKey("userId"))
        
        val user = userRepository.findByEmail("test@example.com")
        assertNotNull(user)
        assertEquals("John", user?.firstName)
        assertEquals("Doe", user?.lastName)
    }
    
    @Test
    fun `should reject duplicate email registration`() {
        val request = RegisterRequest(
            email = "duplicate@example.com",
            password = "password123",
            firstName = "Jane",
            lastName = "Doe",
            roles = setOf(Role.STUDENT)
        )
        
        // First registration
        restTemplate.postForEntity("/api/v1/auth/register", request, Map::class.java)
        
        // Second registration with same email
        val response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            request,
            Map::class.java
        )
        
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }
}
