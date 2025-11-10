package com.opengnosis.gateway.integration

import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

class RouteForwardingIntegrationTest : BaseIntegrationTest() {

    @Test
    fun `should forward IAM service requests correctly`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

        // Setup mock IAM service
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/api/v1/users/user-123")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(200)
                    .withBody("""{"id": "user-123", "email": "test@example.com"}""")
                    .withHeader("Content-Type", "application/json")
            )

        // When: Making request to IAM service endpoint
        webTestClient.get()
            .uri("/api/v1/users/user-123")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Should forward to IAM service and return response
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id").isEqualTo("user-123")
            .jsonPath("$.email").isEqualTo("test@example.com")
    }

    @Test
    fun `should forward Structure service requests correctly`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

        // Setup mock Structure service
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
                    .withBody("""{"schools": [{"id": "school-1", "name": "Test School"}]}""")
                    .withHeader("Content-Type", "application/json")
            )

        // When: Making request to Structure service endpoint
        webTestClient.get()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Should forward to Structure service
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.schools[0].id").isEqualTo("school-1")
            .jsonPath("$.schools[0].name").isEqualTo("Test School")
    }

    @Test
    fun `should forward Scheduler service requests correctly`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

        // Setup mock Scheduler service
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/api/v1/schedules/schedule-123")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(200)
                    .withBody("""{"id": "schedule-123", "entries": []}""")
                    .withHeader("Content-Type", "application/json")
            )

        // When: Making request to Scheduler service endpoint
        webTestClient.get()
            .uri("/api/v1/schedules/schedule-123")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Should forward to Scheduler service
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo("schedule-123")
    }

    @Test
    fun `should forward Journal Command service requests correctly`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

        // Setup mock Journal Command service
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath("/api/v1/commands/grades")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(202)
                    .withBody("""{"commandId": "cmd-123", "status": "ACCEPTED"}""")
                    .withHeader("Content-Type", "application/json")
            )

        // When: Making request to Journal Command service endpoint
        webTestClient.post()
            .uri("/api/v1/commands/grades")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"studentId": "student-1", "subjectId": "subject-1", "gradeValue": 85}""")
            .exchange()
            // Then: Should forward to Journal Command service
            .expectStatus().isAccepted
            .expectBody()
            .jsonPath("$.commandId").isEqualTo("cmd-123")
            .jsonPath("$.status").isEqualTo("ACCEPTED")
    }

    @Test
    fun `should forward Analytics Query service requests correctly`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

        // Setup mock Analytics Query service
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/api/v1/students/student-123/grades")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(200)
                    .withBody("""{"studentId": "student-123", "grades": []}""")
                    .withHeader("Content-Type", "application/json")
            )

        // When: Making request to Analytics Query service endpoint
        webTestClient.get()
            .uri("/api/v1/students/student-123/grades")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Should forward to Analytics Query service
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.studentId").isEqualTo("student-123")
    }

    @Test
    fun `should forward Notifier service requests correctly`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

        // Setup mock Notifier service
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/api/v1/notifications/preferences")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(200)
                    .withBody("""{"emailEnabled": true, "pushEnabled": true}""")
                    .withHeader("Content-Type", "application/json")
            )

        // When: Making request to Notifier service endpoint
        webTestClient.get()
            .uri("/api/v1/notifications/preferences")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Should forward to Notifier service
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.emailEnabled").isEqualTo(true)
    }

    @Test
    fun `should preserve request body when forwarding POST requests`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

        val requestBody = """
            {
                "name": "New School",
                "address": "123 Main St",
                "principalId": "principal-1"
            }
        """.trimIndent()

        // Setup mock Structure service to verify body
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath("/api/v1/schools")
                    .withBody(org.mockserver.model.JsonBody.json(requestBody))
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(201)
                    .withBody("""{"id": "school-123", "name": "New School"}""")
                    .withHeader("Content-Type", "application/json")
            )

        // When: Making POST request with body
        webTestClient.post()
            .uri("/api/v1/schools")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            // Then: Should forward body correctly
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").isEqualTo("school-123")
    }

    @Test
    fun `should preserve query parameters when forwarding requests`() {
        // Given: A valid JWT token
        val validToken = generateValidToken()

        // Setup mock Analytics service
        val mockServerClient = MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        mockServerClient
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/api/v1/search/students")
                    .withQueryStringParameter("query", "John")
                    .withQueryStringParameter("limit", "10")
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(200)
                    .withBody("""{"results": []}""")
                    .withHeader("Content-Type", "application/json")
            )

        // When: Making request with query parameters
        webTestClient.get()
            .uri("/api/v1/search/students?query=John&limit=10")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $validToken")
            .exchange()
            // Then: Should forward query parameters
            .expectStatus().isOk
    }
}
