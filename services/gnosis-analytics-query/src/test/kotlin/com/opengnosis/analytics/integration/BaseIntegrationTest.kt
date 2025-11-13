package com.opengnosis.analytics.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.elasticsearch.ElasticsearchContainer
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
            withDatabaseName("analytics_test_db")
            withUsername("test")
            withPassword("test")
        }
        
        @Container
        val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0")).apply {
            withEmbeddedZookeeper()
        }
        
        @Container
        val elasticsearchContainer = ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")).apply {
            withEnv("xpack.security.enabled", "false")
            withEnv("discovery.type", "single-node")
        }
        
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers)
            registry.add("spring.elasticsearch.uris") { elasticsearchContainer.httpHostAddress }
        }
    }
}
