package com.opengnosis.journal.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.opengnosis.domain.AttendanceStatus
import com.opengnosis.domain.GradeType
import com.opengnosis.events.AttendanceMarkedEvent
import com.opengnosis.events.GradePlacedEvent
import com.opengnosis.journal.controller.MarkAttendanceRequest
import com.opengnosis.journal.controller.PlaceGradeRequest
import com.opengnosis.journal.handler.CommandResponse
import com.opengnosis.journal.repository.CommandAuditLogRepository
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.kafka.support.serializer.JsonDeserializer
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import java.util.Properties

class EventPublishingIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var restTemplate: TestRestTemplate
    
    @Autowired
    private lateinit var commandAuditLogRepository: CommandAuditLogRepository
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @Value("\${kafka.topics.grade-placed}")
    private lateinit var gradePlacedTopic: String
    
    @Value("\${kafka.topics.attendance-marked}")
    private lateinit var attendanceMarkedTopic: String
    
    @BeforeEach
    fun setup() {
        commandAuditLogRepository.deleteAll()
    }
    
    @Test
    fun `should publish GradePlacedEvent to Kafka when grade command is accepted`() {
        // Given
        val studentId = UUID.randomUUID()
        val subjectId = UUID.randomUUID()
        val issuedBy = UUID.randomUUID()
        
        val request = PlaceGradeRequest(
            commandId = UUID.randomUUID(),
            issuedBy = issuedBy,
            studentId = studentId,
            subjectId = subjectId,
            gradeValue = 9,
            gradeType = GradeType.EXAM,
            comment = "Excellent work"
        )
        
        // Set up Kafka consumer
        val consumerProps = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer::class.java)
            put(JsonDeserializer.TRUSTED_PACKAGES, "*")
            put(JsonDeserializer.VALUE_DEFAULT_TYPE, GradePlacedEvent::class.java.name)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
        }
        
        val receiverOptions: ReceiverOptions<String, GradePlacedEvent> = ReceiverOptions.create(consumerProps)
        val receiverOptionsWithSubscription = receiverOptions.subscription(listOf(gradePlacedTopic))
        
        val receiver = KafkaReceiver.create(receiverOptionsWithSubscription)
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/grades",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("ACCEPTED", response.body!!.status)
        
        // Verify event was published to Kafka
        val receivedEvent = receiver.receive()
            .blockFirst(Duration.ofSeconds(10))
        
        assertNotNull(receivedEvent, "Event should be published to Kafka")
        val event = receivedEvent!!.value()
        
        assertEquals(studentId, event.studentId)
        assertEquals(subjectId, event.subjectId)
        assertEquals(9, event.gradeValue)
        assertEquals(GradeType.EXAM, event.gradeType)
        assertEquals("Excellent work", event.comment)
        assertEquals(issuedBy, event.placedBy)
        assertEquals(studentId, event.aggregateId)
    }
    
    @Test
    fun `should publish AttendanceMarkedEvent to Kafka when attendance command is accepted`() {
        // Given
        val studentId = UUID.randomUUID()
        val classId = UUID.randomUUID()
        val issuedBy = UUID.randomUUID()
        val date = LocalDate.now()
        
        val request = MarkAttendanceRequest(
            commandId = UUID.randomUUID(),
            issuedBy = issuedBy,
            studentId = studentId,
            classId = classId,
            date = date,
            lessonNumber = 5,
            status = AttendanceStatus.PRESENT
        )
        
        // Set up Kafka consumer
        val consumerProps = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-attendance-consumer-group")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer::class.java)
            put(JsonDeserializer.TRUSTED_PACKAGES, "*")
            put(JsonDeserializer.VALUE_DEFAULT_TYPE, AttendanceMarkedEvent::class.java.name)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
        }
        
        val receiverOptions: ReceiverOptions<String, AttendanceMarkedEvent> = ReceiverOptions.create(consumerProps)
        val receiverOptionsWithSubscription = receiverOptions.subscription(listOf(attendanceMarkedTopic))
        
        val receiver = KafkaReceiver.create(receiverOptionsWithSubscription)
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/attendance",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("ACCEPTED", response.body!!.status)
        
        // Verify event was published to Kafka
        val receivedEvent = receiver.receive()
            .blockFirst(Duration.ofSeconds(10))
        
        assertNotNull(receivedEvent, "Event should be published to Kafka")
        val event = receivedEvent!!.value()
        
        assertEquals(studentId, event.studentId)
        assertEquals(classId, event.classId)
        assertEquals(date, event.date)
        assertEquals(5, event.lessonNumber)
        assertEquals(AttendanceStatus.PRESENT, event.status)
        assertEquals(issuedBy, event.markedBy)
        assertEquals(studentId, event.aggregateId)
    }
    
    @Test
    fun `should not publish event to Kafka when grade command is rejected`() {
        // Given - invalid grade value
        val request = PlaceGradeRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            subjectId = UUID.randomUUID(),
            gradeValue = 15, // Invalid
            gradeType = GradeType.EXAM,
            comment = null
        )
        
        // Set up Kafka consumer
        val consumerProps = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-rejected-consumer-group")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer::class.java)
            put(JsonDeserializer.TRUSTED_PACKAGES, "*")
            put(JsonDeserializer.VALUE_DEFAULT_TYPE, GradePlacedEvent::class.java.name)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
        }
        
        val receiverOptions: ReceiverOptions<String, GradePlacedEvent> = ReceiverOptions.create(consumerProps)
        val receiverOptionsWithSubscription = receiverOptions.subscription(listOf(gradePlacedTopic))
        
        val receiver = KafkaReceiver.create(receiverOptionsWithSubscription)
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/grades",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("REJECTED", response.body!!.status)
        
        // Verify no event was published to Kafka
        val receivedEvent = receiver.receive()
            .blockFirst(Duration.ofSeconds(3))
        
        assertNull(receivedEvent, "No event should be published for rejected command")
    }
    
    @Test
    fun `should not publish event to Kafka when attendance command is rejected`() {
        // Given - future date (invalid)
        val request = MarkAttendanceRequest(
            commandId = UUID.randomUUID(),
            issuedBy = UUID.randomUUID(),
            studentId = UUID.randomUUID(),
            classId = UUID.randomUUID(),
            date = LocalDate.now().plusDays(5), // Future date
            lessonNumber = 3,
            status = AttendanceStatus.PRESENT
        )
        
        // Set up Kafka consumer
        val consumerProps = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "test-rejected-attendance-consumer-group")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer::class.java)
            put(JsonDeserializer.TRUSTED_PACKAGES, "*")
            put(JsonDeserializer.VALUE_DEFAULT_TYPE, AttendanceMarkedEvent::class.java.name)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
        }
        
        val receiverOptions: ReceiverOptions<String, AttendanceMarkedEvent> = ReceiverOptions.create(consumerProps)
        val receiverOptionsWithSubscription = receiverOptions.subscription(listOf(attendanceMarkedTopic))
        
        val receiver = KafkaReceiver.create(receiverOptionsWithSubscription)
        
        // When
        val response: ResponseEntity<CommandResponse> = restTemplate.postForEntity(
            "/api/v1/commands/attendance",
            request,
            CommandResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("REJECTED", response.body!!.status)
        
        // Verify no event was published to Kafka
        val receivedEvent = receiver.receive()
            .blockFirst(Duration.ofSeconds(3))
        
        assertNull(receivedEvent, "No event should be published for rejected command")
    }
}
