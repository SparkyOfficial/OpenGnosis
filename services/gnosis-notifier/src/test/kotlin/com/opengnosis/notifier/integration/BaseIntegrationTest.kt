package com.opengnosis.notifier.integration

import com.icegreen.greenmail.configuration.GreenMailConfiguration
import com.icegreen.greenmail.junit5.GreenMailExtension
import com.icegreen.greenmail.util.ServerSetupTest
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
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
            withDatabaseName("notifier_test_db")
            withUsername("test")
            withPassword("test")
        }
        
        @Container
        val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0")).apply {
            withEmbeddedZookeeper()
        }
        
        @JvmField
        @RegisterExtension
        val greenMail: GreenMailExtension = GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "test"))
            .withPerMethodLifecycle(false)
        
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers)
            
            // Configure GreenMail SMTP
            registry.add("spring.mail.host") { "localhost" }
            registry.add("spring.mail.port") { ServerSetupTest.SMTP.port }
            registry.add("spring.mail.username") { "test" }
            registry.add("spring.mail.password") { "test" }
            registry.add("spring.mail.properties.mail.smtp.auth") { "true" }
            registry.add("spring.mail.properties.mail.smtp.starttls.enable") { "false" }
            
            // Enable email notifications for tests
            registry.add("notification.email.enabled") { "true" }
            registry.add("notification.email.from") { "test@opengnosis.com" }
            
            // Disable push and SMS for most tests
            registry.add("notification.push.enabled") { "false" }
            registry.add("notification.sms.enabled") { "false" }
        }
    }
}
