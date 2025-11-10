package com.opengnosis.gateway.integration

import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

class RateLimitIntegrationTest : BaseIntegrationTest() {

    @Test
    fun `should enforce rate limit of 100 requests per minute`() {
        // Given: A valid JWT token
        val validToken = generateValidToken(userId = "rate-limit-user")

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
            )

        // When: Making 100 requests (should all succeed)
        repeat(100) { i ->
            webTestClient.get()
                .uri("/api/v1/schools")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
                .exchange()
                .expectStatus().isOk
                .expectHeader().exists("X-RateLimit-Limit")
                .expectHeader().valueEquals("X-RateLimit-Limit", "100")
                .expectHeader().exists("X-RateLimit-Remaining")
                .expectHeader().exists("X-RateLimit-Reset")
        }

        // When: Making the 101st request
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Should return 429 Too Many Requests
            .expectStatus().isEqualTo(429)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectHeader().valueEquals("X-RateLimit-Limit", "100")
            .expectHeader().valueEquals("X-RateLimit-Remaining", "0")
            .expectHeader().exists("X-RateLimit-Reset")
            .expectBody()
            .jsonPath("$.status").isEqualTo(429)
            .jsonPath("$.error").isEqualTo("Too Many Requests")
            .jsonPath("$.message").value<String> { message ->
                assert(message.contains("Rate limit exceeded"))
            }
    }

    @Test
    fun `should add rate limit headers to successful responses`() {
        // Given: A valid JWT token
        val validToken = generateValidToken(userId = "header-test-user")

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
            )

        // When: Making a request
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Should include rate limit headers
            .expectStatus().isOk
            .expectHeader().exists("X-RateLimit-Limit")
            .expectHeader().valueEquals("X-RateLimit-Limit", "100")
            .expectHeader().exists("X-RateLimit-Remaining")
            .expectHeader().exists("X-RateLimit-Reset")
    }

    @Test
    fun `should not apply rate limiting to public paths`() {
        // Setup mock backend service for login
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath("/api/v1/auth/login")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(200)
                    .withBody("""{"token": "test-token"}""")
            )

        // When: Making more than 100 requests to public endpoint
        repeat(105) {
            webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"email": "test@example.com", "password": "password"}""")
                .exchange()
                // Then: Should not be rate limited
                .expectStatus().isNotEqualTo(429)
        }
    }

    @Test
    fun `should track rate limits per user independently`() {
        // Given: Two different users
        val token1 = generateValidToken(userId = "user-1")
        val token2 = generateValidToken(userId = "user-2")

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
            )

        // When: User 1 makes 100 requests
        repeat(100) {
            webTestClient.get()
                .uri("/api/v1/schools")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token1")
                .exchange()
                .expectStatus().isOk
        }

        // And: User 1 exceeds limit
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token1")
            .exchange()
            .expectStatus().isEqualTo(429)

        // Then: User 2 should still be able to make requests
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token2")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `should decrement remaining count with each request`() {
        // Given: A valid JWT token
        val validToken = generateValidToken(userId = "decrement-test-user")

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
            )

        // When: Making first request
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("X-RateLimit-Remaining", "99")

        // When: Making second request
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("X-RateLimit-Remaining", "98")

        // When: Making third request
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("X-RateLimit-Remaining", "97")
    }
}
