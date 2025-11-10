package com.opengnosis.gateway.integration

import com.opengnosis.common.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.core.publisher.Mono
import java.time.Duration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.profiles.active=test"]
)
@Testcontainers
abstract class BaseIntegrationTest {

    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @Autowired
    protected lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    protected lateinit var redisTemplate: ReactiveStringRedisTemplate

    companion object {
        @Container
        val redisContainer = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .apply {
                withExposedPorts(6379)
            }

        @Container
        val mockServerContainer = MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"))

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host", redisContainer::getHost)
            registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort)
            registry.add("MOCK_SERVER_URL") { "http://${mockServerContainer.host}:${mockServerContainer.serverPort}" }
        }
    }

    @BeforeEach
    fun setUp() {
        // Clear Redis before each test
        redisTemplate.execute { connection ->
            connection.serverCommands().flushAll()
        }.blockLast(Duration.ofSeconds(5))

        // Configure WebTestClient with longer timeout for integration tests
        webTestClient = webTestClient.mutate()
            .responseTimeout(Duration.ofSeconds(10))
            .build()
    }

    protected fun generateValidToken(userId: String = "test-user-id", email: String = "test@example.com", roles: String = "STUDENT"): String {
        return jwtTokenProvider.generateToken(userId, email, setOf(roles))
    }

    protected fun generateExpiredToken(userId: String = "test-user-id", email: String = "test@example.com", roles: String = "STUDENT"): String {
        return jwtTokenProvider.generateToken(userId, email, setOf(roles), -1000)
    }
}
