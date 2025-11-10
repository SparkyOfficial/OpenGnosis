package com.opengnosis.gateway.integration

import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class JwtAuthenticationIntegrationTest : BaseIntegrationTest() {

    @Test
    fun `should allow request with valid JWT token`() {
        // Given: A valid JWT token
        val validToken = generateValidToken(
            userId = "user-123",
            email = "student@example.com",
            roles = "STUDENT"
        )

        // Setup mock backend service
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/api/v1/schools")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(200)
                    .withBody("""{"schools": []}""")
                    .withHeader("Content-Type", "application/json")
            )

        // When: Making a request with valid token
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Request should be successful
            .expectStatus().isOk
    }

    @Test
    fun `should reject request without authorization header`() {
        // When: Making a request without authorization header
        webTestClient.get()
            .uri("/api/v1/schools")
            .exchange()
            // Then: Should return 401 Unauthorized
            .expectStatus().isUnauthorized
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(401)
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.message").value<String> { message ->
                assert(message.contains("Missing or invalid Authorization header"))
            }
    }

    @Test
    fun `should reject request with invalid JWT token`() {
        // Given: An invalid JWT token
        val invalidToken = "invalid.jwt.token"

        // When: Making a request with invalid token
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $invalidToken")
            .exchange()
            // Then: Should return 401 Unauthorized
            .expectStatus().isUnauthorized
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(401)
            .jsonPath("$.error").isEqualTo("Unauthorized")
    }

    @Test
    fun `should reject request with expired JWT token`() {
        // Given: An expired JWT token
        val expiredToken = generateExpiredToken(
            userId = "user-123",
            email = "test@example.com",
            roles = "STUDENT"
        )

        // When: Making a request with expired token
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $expiredToken")
            .exchange()
            // Then: Should return 401 Unauthorized
            .expectStatus().isUnauthorized
            .expectBody()
            .jsonPath("$.status").isEqualTo(401)
            .jsonPath("$.error").isEqualTo("Unauthorized")
    }

    @Test
    fun `should allow public paths without authentication`() {
        // When: Making requests to public paths without token
        // Login endpoint
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"email": "test@example.com", "password": "password"}""")
            .exchange()
            // Then: Should not return 401 (may return 404 or other status from backend)
            .expectStatus().isNotEqualTo(HttpStatus.UNAUTHORIZED)

        // Register endpoint
        webTestClient.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"email": "test@example.com", "password": "password"}""")
            .exchange()
            .expectStatus().isNotEqualTo(HttpStatus.UNAUTHORIZED)

        // Health endpoint
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isNotEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `should propagate user context to downstream services`() {
        // Given: A valid JWT token with user information
        val validToken = generateValidToken(
            userId = "user-456",
            email = "teacher@example.com",
            roles = "TEACHER"
        )

        // Setup mock backend to verify headers
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/api/v1/classes")
                    .withHeader("X-User-Id", "user-456")
                    .withHeader("X-User-Email", "teacher@example.com")
                    .withHeader("X-User-Roles", "TEACHER")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(200)
                    .withBody("""{"classes": []}""")
            )

        // When: Making a request with valid token
        webTestClient.get()
            .uri("/api/v1/classes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Request should be successful (mock server verifies headers)
            .expectStatus().isOk
    }

    @Test
    fun `should reject request with malformed authorization header`() {
        // When: Making a request with malformed authorization header (missing Bearer prefix)
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "InvalidPrefix token")
            .exchange()
            // Then: Should return 401 Unauthorized
            .expectStatus().isUnauthorized
            .expectBody()
            .jsonPath("$.status").isEqualTo(401)
            .jsonPath("$.message").value<String> { message ->
                assert(message.contains("Missing or invalid Authorization header"))
            }
    }
}
