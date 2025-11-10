package com.opengnosis.structure.integration

import com.opengnosis.events.ClassCreatedEvent
import com.opengnosis.events.SchoolCreatedEvent
import com.opengnosis.events.StudentEnrolledEvent
import com.opengnosis.events.StudentUnenrolledEvent
import com.opengnosis.structure.dto.CreateClassRequest
import com.opengnosis.structure.dto.CreateSchoolRequest
import com.opengnosis.structure.dto.EnrollStudentRequest
import com.opengnosis.structure.dto.UnenrollStudentRequest
import com.opengnosis.structure.service.ClassService
import com.opengnosis.structure.service.EnrollmentService
import com.opengnosis.structure.service.SchoolService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.test.utils.ContainerTestUtils
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class EventPublishingIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var schoolService: SchoolService
    
    @Autowired
    private lateinit var classService: ClassService
    
    @Autowired
    private lateinit var enrollmentService: EnrollmentService
    
    private lateinit var schoolEventQueue: BlockingQueue<ConsumerRecord<String, SchoolCreatedEvent>>
    private lateinit var classEventQueue: BlockingQueue<ConsumerRecord<String, ClassCreatedEvent>>
    private lateinit var enrolledEventQueue: BlockingQueue<ConsumerRecord<String, StudentEnrolledEvent>>
    private lateinit var unenrolledEventQueue: BlockingQueue<ConsumerRecord<String, StudentUnenrolledEvent>>
    
    private lateinit var schoolEventContainer: KafkaMessageListenerContainer<String, SchoolCreatedEvent>
    private lateinit var classEventContainer: KafkaMessageListenerContainer<String, ClassCreatedEvent>
    private lateinit var enrolledEventContainer: KafkaMessageListenerContainer<String, StudentEnrolledEvent>
    private lateinit var unenrolledEventContainer: KafkaMessageListenerContainer<String, StudentUnenrolledEvent>
    
    @BeforeEach
    fun setup() {
        schoolEventQueue = LinkedBlockingQueue()
        classEventQueue = LinkedBlockingQueue()
        enrolledEventQueue = LinkedBlockingQueue()
        unenrolledEventQueue = LinkedBlockingQueue()
        
        setupSchoolEventConsumer()
        setupClassEventConsumer()
        setupEnrolledEventConsumer()
        setupUnenrolledEventConsumer()
    }
    
    @Test
    fun `should publish SchoolCreatedEvent when school is created`() {
        // Given
        val principalId = UUID.randomUUID()
        val request = CreateSchoolRequest(
            name = "Event Test School",
            address = "123 Event Street",
            principalId = principalId
        )
        
        // When
        val school = schoolService.createSchool(request)
        
        // Then
        val record = schoolEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(record, "SchoolCreatedEvent should be published")
        
        val event = record!!.value()
        assertEquals(school.id, event.aggregateId)
        assertEquals("Event Test School", event.name)
        assertEquals("123 Event Street", event.address)
        assertEquals(principalId, event.principalId)
        assertEquals("SchoolCreated", event.eventType)
    }
    
    @Test
    fun `should publish ClassCreatedEvent when class is created`() {
        // Given
        val school = createTestSchool()
        val academicYearId = UUID.randomUUID()
        val classTeacherId = UUID.randomUUID()
        
        val request = CreateClassRequest(
            schoolId = school.id,
            academicYearId = academicYearId,
            name = "Event Test Class",
            grade = 10,
            classTeacherId = classTeacherId,
            capacity = 30
        )
        
        // When
        val classResponse = classService.createClass(request)
        
        // Then
        val record = classEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(record, "ClassCreatedEvent should be published")
        
        val event = record!!.value()
        assertEquals(classResponse.id, event.aggregateId)
        assertEquals(school.id, event.schoolId)
        assertEquals(academicYearId, event.academicYearId)
        assertEquals("Event Test Class", event.name)
        assertEquals(10, event.grade)
        assertEquals(classTeacherId, event.classTeacherId)
        assertEquals("ClassCreated", event.eventType)
    }
    
    @Test
    fun `should publish StudentEnrolledEvent when student is enrolled`() {
        // Given
        val school = createTestSchool()
        val classEntity = createTestClass(school.id)
        val studentId = UUID.randomUUID()
        
        val request = EnrollStudentRequest(
            studentId = studentId,
            classId = classEntity.id
        )
        
        // When
        val enrollment = enrollmentService.enrollStudent(request)
        
        // Then
        val record = enrolledEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(record, "StudentEnrolledEvent should be published")
        
        val event = record!!.value()
        assertEquals(enrollment.id, event.aggregateId)
        assertEquals(studentId, event.studentId)
        assertEquals(classEntity.id, event.classId)
        assertNotNull(event.enrollmentDate)
        assertEquals("StudentEnrolled", event.eventType)
    }
    
    @Test
    fun `should publish StudentUnenrolledEvent when student is unenrolled`() {
        // Given
        val school = createTestSchool()
        val classEntity = createTestClass(school.id)
        val studentId = UUID.randomUUID()
        
        val enrollment = enrollmentService.enrollStudent(
            EnrollStudentRequest(studentId, classEntity.id)
        )
        
        // Clear the enrolled event from queue
        enrolledEventQueue.poll(5, TimeUnit.SECONDS)
        
        // When
        enrollmentService.unenrollStudent(
            enrollment.id,
            UnenrollStudentRequest(reason = "Test withdrawal")
        )
        
        // Then
        val record = unenrolledEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(record, "StudentUnenrolledEvent should be published")
        
        val event = record!!.value()
        assertEquals(enrollment.id, event.aggregateId)
        assertEquals(studentId, event.studentId)
        assertEquals(classEntity.id, event.classId)
        assertEquals("Test withdrawal", event.reason)
        assertEquals("StudentUnenrolled", event.eventType)
    }
    
    @Test
    fun `should publish multiple events in sequence`() {
        // Given
        val principalId = UUID.randomUUID()
        
        // When - Create school
        val school = schoolService.createSchool(
            CreateSchoolRequest("Multi Event School", "456 Multi Street", principalId)
        )
        
        // Then - Verify school event
        val schoolRecord = schoolEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(schoolRecord)
        assertEquals(school.id, schoolRecord!!.value().aggregateId)
        
        // When - Create class
        val classEntity = classService.createClass(
            CreateClassRequest(
                schoolId = school.id,
                academicYearId = UUID.randomUUID(),
                name = "Multi Event Class",
                grade = 9,
                classTeacherId = UUID.randomUUID()
            )
        )
        
        // Then - Verify class event
        val classRecord = classEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(classRecord)
        assertEquals(classEntity.id, classRecord!!.value().aggregateId)
        
        // When - Enroll student
        val studentId = UUID.randomUUID()
        val enrollment = enrollmentService.enrollStudent(
            EnrollStudentRequest(studentId, classEntity.id)
        )
        
        // Then - Verify enrollment event
        val enrollRecord = enrolledEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(enrollRecord)
        assertEquals(enrollment.id, enrollRecord!!.value().aggregateId)
    }
    
    // Helper methods
    private fun createTestSchool() = schoolService.createSchool(
        CreateSchoolRequest(
            name = "Test School ${UUID.randomUUID()}",
            address = "Test Address",
            principalId = UUID.randomUUID()
        )
    ).also {
        // Consume the event to clear the queue
        schoolEventQueue.poll(5, TimeUnit.SECONDS)
    }
    
    private fun createTestClass(schoolId: UUID) = classService.createClass(
        CreateClassRequest(
            schoolId = schoolId,
            academicYearId = UUID.randomUUID(),
            name = "Test Class ${UUID.randomUUID()}",
            grade = 10,
            classTeacherId = UUID.randomUUID(),
            capacity = 30
        )
    ).also {
        // Consume the event to clear the queue
        classEventQueue.poll(5, TimeUnit.SECONDS)
    }
    
    private fun setupSchoolEventConsumer() {
        val consumerProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "test-school-consumer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "com.opengnosis.events",
            JsonDeserializer.VALUE_DEFAULT_TYPE to SchoolCreatedEvent::class.java.name
        )
        
        val consumerFactory = DefaultKafkaConsumerFactory<String, SchoolCreatedEvent>(consumerProps)
        val containerProperties = ContainerProperties("school-events")
        containerProperties.messageListener = MessageListener<String, SchoolCreatedEvent> { record ->
            schoolEventQueue.add(record)
        }
        
        schoolEventContainer = KafkaMessageListenerContainer(consumerFactory, containerProperties)
        schoolEventContainer.start()
        ContainerTestUtils.waitForAssignment(schoolEventContainer, 1)
    }
    
    private fun setupClassEventConsumer() {
        val consumerProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "test-class-consumer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "com.opengnosis.events",
            JsonDeserializer.VALUE_DEFAULT_TYPE to ClassCreatedEvent::class.java.name
        )
        
        val consumerFactory = DefaultKafkaConsumerFactory<String, ClassCreatedEvent>(consumerProps)
        val containerProperties = ContainerProperties("structure-events")
        containerProperties.messageListener = MessageListener<String, ClassCreatedEvent> { record ->
            classEventQueue.add(record)
        }
        
        classEventContainer = KafkaMessageListenerContainer(consumerFactory, containerProperties)
        classEventContainer.start()
        ContainerTestUtils.waitForAssignment(classEventContainer, 1)
    }
    
    private fun setupEnrolledEventConsumer() {
        val consumerProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "test-enrolled-consumer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "com.opengnosis.events",
            JsonDeserializer.VALUE_DEFAULT_TYPE to StudentEnrolledEvent::class.java.name
        )
        
        val consumerFactory = DefaultKafkaConsumerFactory<String, StudentEnrolledEvent>(consumerProps)
        val containerProperties = ContainerProperties("enrollment-events")
        containerProperties.messageListener = MessageListener<String, StudentEnrolledEvent> { record ->
            enrolledEventQueue.add(record)
        }
        
        enrolledEventContainer = KafkaMessageListenerContainer(consumerFactory, containerProperties)
        enrolledEventContainer.start()
        ContainerTestUtils.waitForAssignment(enrolledEventContainer, 1)
    }
    
    private fun setupUnenrolledEventConsumer() {
        val consumerProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "test-unenrolled-consumer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "com.opengnosis.events",
            JsonDeserializer.VALUE_DEFAULT_TYPE to StudentUnenrolledEvent::class.java.name
        )
        
        val consumerFactory = DefaultKafkaConsumerFactory<String, StudentUnenrolledEvent>(consumerProps)
        val containerProperties = ContainerProperties("enrollment-events")
        containerProperties.messageListener = MessageListener<String, StudentUnenrolledEvent> { record ->
            unenrolledEventQueue.add(record)
        }
        
        unenrolledEventContainer = KafkaMessageListenerContainer(consumerFactory, containerProperties)
        unenrolledEventContainer.start()
        ContainerTestUtils.waitForAssignment(unenrolledEventContainer, 1)
    }
}
