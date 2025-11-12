package com.opengnosis.journal.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@Testcontainers
abstract class BaseIntegrationTest {
    
    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:15-alpine")).apply {
            withDatabaseName("journal_command_test_db")
            withUsername("test")
            withPassword("test")
        }
        
        @Container
        val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0")).apply {
            withEmbeddedZookeeper()
        }
        
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers)
        }
    }
}
