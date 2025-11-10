package com.opengnosis.gateway.integration

import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType

class CorsConfigurationIntegrationTest : BaseIntegrationTest() {

    @Test
    fun `should handle CORS preflight request`() {
        // When: Making OPTIONS preflight request
        webTestClient.options()
            .uri("/api/v1/schools")
            .header(HttpHeaders.ORIGIN, "http://localhost:3000")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type")
            .exchange()
            // Then: Should return CORS headers
            .expectStatus().isOk
            .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
            .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
            .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)
            .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_MAX_AGE)
    }

    @Test
    fun `should include CORS headers in actual request response`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

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

        // When: Making actual request with Origin header
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.ORIGIN, "http://localhost:3000")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Should include CORS headers
            .expectStatus().isOk
            .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
    }

    @Test
    fun `should allow GET requests from any origin`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

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

        // When: Making GET request from different origins
        listOf(
            "http://localhost:3000",
            "https://example.com",
            "https://app.opengnosis.com"
        ).forEach { origin ->
            webTestClient.get()
                .uri("/api/v1/schools")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
                .exchange()
                // Then: Should allow request
                .expectStatus().isOk
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
        }
    }

    @Test
    fun `should allow POST requests with CORS`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

        // Setup mock backend service
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath("/api/v1/schools")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(201)
                    .withBody("""{"id": "school-123"}""")
            )

        // When: Making POST request with Origin header
        webTestClient.post()
            .uri("/api/v1/schools")
            .header(HttpHeaders.ORIGIN, "http://localhost:3000")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"name": "Test School"}""")
            .exchange()
            // Then: Should allow request and include CORS headers
            .expectStatus().isCreated
            .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
    }

    @Test
    fun `should allow PUT requests with CORS`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

        // Setup mock backend service
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("PUT")
                    .withPath("/api/v1/schools/school-123")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(200)
                    .withBody("""{"id": "school-123", "name": "Updated School"}""")
            )

        // When: Making PUT request with Origin header
        webTestClient.put()
            .uri("/api/v1/schools/school-123")
            .header(HttpHeaders.ORIGIN, "http://localhost:3000")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"name": "Updated School"}""")
            .exchange()
            // Then: Should allow request
            .expectStatus().isOk
            .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
    }

    @Test
    fun `should allow DELETE requests with CORS`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

        // Setup mock backend service
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("DELETE")
                    .withPath("/api/v1/schools/school-123")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(204)
            )

        // When: Making DELETE request with Origin header
        webTestClient.delete()
            .uri("/api/v1/schools/school-123")
            .header(HttpHeaders.ORIGIN, "http://localhost:3000")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Should allow request
            .expectStatus().isNoContent
            .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
    }

    @Test
    fun `should expose rate limit headers in CORS`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

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

        // When: Making request with Origin header
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.ORIGIN, "http://localhost:3000")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Should expose rate limit headers
            .expectStatus().isOk
            .expectHeader().exists("X-RateLimit-Remaining")
            .expectHeader().exists("X-RateLimit-Limit")
            .expectHeader().exists("X-RateLimit-Reset")
    }

    @Test
    fun `should set max age for preflight cache`() {
        // When: Making OPTIONS preflight request
        webTestClient.options()
            .uri("/api/v1/schools")
            .header(HttpHeaders.ORIGIN, "http://localhost:3000")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            .exchange()
            // Then: Should set max age to 3600 seconds (1 hour)
            .expectStatus().isOk
            .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600")
    }

    @Test
    fun `should allow all headers in CORS requests`() {
        // When: Making preflight request with custom headers
        webTestClient.options()
            .uri("/api/v1/schools")
            .header(HttpHeaders.ORIGIN, "http://localhost:3000")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type, X-Custom-Header")
            .exchange()
            // Then: Should allow all requested headers
            .expectStatus().isOk
            .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)
    }
}
