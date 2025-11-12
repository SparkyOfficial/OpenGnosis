package com.opengnosis.scheduler.integration

import com.opengnosis.events.ScheduleCreatedEvent
import com.opengnosis.events.ScheduleModifiedEvent
import com.opengnosis.scheduler.dto.CreateScheduleEntryRequest
import com.opengnosis.scheduler.dto.CreateScheduleRequest
import com.opengnosis.scheduler.entity.Classroom
import com.opengnosis.scheduler.entity.TeacherAvailability
import com.opengnosis.scheduler.repository.ClassroomRepository
import com.opengnosis.scheduler.repository.TeacherAvailabilityRepository
import com.opengnosis.scheduler.service.ScheduleService
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
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class EventPublishingIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var scheduleService: ScheduleService
    
    @Autowired
    private lateinit var classroomRepository: ClassroomRepository
    
    @Autowired
    private lateinit var teacherAvailabilityRepository: TeacherAvailabilityRepository
    
    private lateinit var scheduleCreatedEventQueue: BlockingQueue<ConsumerRecord<String, ScheduleCreatedEvent>>
    private lateinit var scheduleModifiedEventQueue: BlockingQueue<ConsumerRecord<String, ScheduleModifiedEvent>>
    
    private lateinit var scheduleCreatedEventContainer: KafkaMessageListenerContainer<String, ScheduleCreatedEvent>
    private lateinit var scheduleModifiedEventContainer: KafkaMessageListenerContainer<String, ScheduleModifiedEvent>
    
    private lateinit var testClassroomId: UUID
    private lateinit var testTeacherId: UUID
    
    @BeforeEach
    fun setup() {
        scheduleCreatedEventQueue = LinkedBlockingQueue()
        scheduleModifiedEventQueue = LinkedBlockingQueue()
        
        setupScheduleCreatedEventConsumer()
        setupScheduleModifiedEventConsumer()
        
        // Create test data
        testClassroomId = UUID.randomUUID()
        testTeacherId = UUID.randomUUID()
        
        classroomRepository.save(
            Classroom(
                id = testClassroomId,
                schoolId = UUID.randomUUID(),
                name = "Room 101",
                capacity = 30
            )
        )
        
        val days = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
        days.forEach { day ->
            teacherAvailabilityRepository.save(
                TeacherAvailability(
                    teacherId = testTeacherId,
                    dayOfWeek = day,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0)
                )
            )
        }
    }
    
    @Test
    fun `should publish ScheduleCreatedEvent when schedule is created`() {
        // Given
        val academicYearId = UUID.randomUUID()
        val termId = UUID.randomUUID()
        val request = CreateScheduleRequest(
            academicYearId = academicYearId,
            termId = termId
        )
        
        // When
        val schedule = scheduleService.createSchedule(request)
        
        // Then
        val record = scheduleCreatedEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(record, "ScheduleCreatedEvent should be published")
        
        val event = record!!.value()
        assertEquals(schedule.id, event.aggregateId)
        assertEquals(academicYearId, event.academicYearId)
        assertEquals(termId, event.termId)
        assertEquals("ScheduleCreated", event.eventType)
        assertNotNull(event.createdBy)
        assertNotNull(event.timestamp)
    }
    
    @Test
    fun `should publish ScheduleModifiedEvent when schedule entry is added`() {
        // Given
        val schedule = createTestSchedule()
        
        val classId = UUID.randomUUID()
        val subjectId = UUID.randomUUID()
        
        val entryRequest = CreateScheduleEntryRequest(
            classId = classId,
            subjectId = subjectId,
            teacherId = testTeacherId,
            classroomId = testClassroomId,
            dayOfWeek = "MONDAY",
            startTime = "09:00",
            endTime = "10:00"
        )
        
        // When
        val entry = scheduleService.addScheduleEntry(schedule.id, entryRequest)
        
        // Then
        val record = scheduleModifiedEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(record, "ScheduleModifiedEvent should be published")
        
        val event = record!!.value()
        assertEquals(schedule.id, event.aggregateId)
        assertEquals(entry.id, event.scheduleEntryId)
        assertEquals(classId, event.classId)
        assertEquals(subjectId, event.subjectId)
        assertEquals(testTeacherId, event.teacherId)
        assertEquals(testClassroomId, event.classroomId)
        assertEquals(DayOfWeek.MONDAY, event.dayOfWeek)
        assertEquals(LocalTime.of(9, 0), event.startTime)
        assertEquals(LocalTime.of(10, 0), event.endTime)
        assertEquals("ScheduleModified", event.eventType)
        assertNotNull(event.modifiedBy)
    }
    
    @Test
    fun `should publish ScheduleModifiedEvent when schedule entry is updated`() {
        // Given
        val schedule = createTestSchedule()
        
        val originalEntry = scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "TUESDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        // Clear the queue from the add event
        scheduleModifiedEventQueue.poll(5, TimeUnit.SECONDS)
        
        // When
        val updateRequest = CreateScheduleEntryRequest(
            classId = originalEntry.classId,
            subjectId = originalEntry.subjectId,
            teacherId = testTeacherId,
            classroomId = testClassroomId,
            dayOfWeek = "TUESDAY",
            startTime = "11:00", // Changed time
            endTime = "12:00"
        )
        
        scheduleService.updateScheduleEntry(schedule.id, originalEntry.id, updateRequest)
        
        // Then
        val record = scheduleModifiedEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(record, "ScheduleModifiedEvent should be published on update")
        
        val event = record!!.value()
        assertEquals(schedule.id, event.aggregateId)
        assertEquals(originalEntry.id, event.scheduleEntryId)
        assertEquals(LocalTime.of(11, 0), event.startTime)
        assertEquals(LocalTime.of(12, 0), event.endTime)
    }
    
    @Test
    fun `should publish ScheduleModifiedEvent when schedule entry is deleted`() {
        // Given
        val schedule = createTestSchedule()
        
        val entry = scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "WEDNESDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        // Clear the queue from the add event
        scheduleModifiedEventQueue.poll(5, TimeUnit.SECONDS)
        
        // When
        scheduleService.deleteScheduleEntry(schedule.id, entry.id)
        
        // Then
        val record = scheduleModifiedEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(record, "ScheduleModifiedEvent should be published on delete")
        
        val event = record!!.value()
        assertEquals(schedule.id, event.aggregateId)
        assertEquals(entry.id, event.scheduleEntryId)
    }
    
    @Test
    fun `should publish multiple events in sequence`() {
        // Given
        val academicYearId = UUID.randomUUID()
        val termId = UUID.randomUUID()
        
        // When - Create schedule
        val schedule = scheduleService.createSchedule(
            CreateScheduleRequest(academicYearId, termId)
        )
        
        // Then - Verify schedule created event
        val createdRecord = scheduleCreatedEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(createdRecord)
        assertEquals(schedule.id, createdRecord!!.value().aggregateId)
        
        // When - Add first entry
        val entry1 = scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "THURSDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        // Then - Verify first modified event
        val modified1Record = scheduleModifiedEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(modified1Record)
        assertEquals(entry1.id, modified1Record!!.value().scheduleEntryId)
        
        // When - Add second entry
        val entry2 = scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "FRIDAY",
                startTime = "10:00",
                endTime = "11:00"
            )
        )
        
        // Then - Verify second modified event
        val modified2Record = scheduleModifiedEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(modified2Record)
        assertEquals(entry2.id, modified2Record!!.value().scheduleEntryId)
    }
    
    @Test
    fun `should include all required fields in published events`() {
        // Given
        val schedule = createTestSchedule()
        
        // Clear the created event
        scheduleCreatedEventQueue.poll(5, TimeUnit.SECONDS)
        
        // When
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "MONDAY",
                startTime = "14:00",
                endTime = "15:00"
            )
        )
        
        // Then
        val record = scheduleModifiedEventQueue.poll(10, TimeUnit.SECONDS)
        assertNotNull(record)
        
        val event = record!!.value()
        assertNotNull(event.eventId)
        assertNotNull(event.aggregateId)
        assertNotNull(event.scheduleEntryId)
        assertNotNull(event.classId)
        assertNotNull(event.subjectId)
        assertNotNull(event.teacherId)
        assertNotNull(event.classroomId)
        assertNotNull(event.dayOfWeek)
        assertNotNull(event.startTime)
        assertNotNull(event.endTime)
        assertNotNull(event.modifiedBy)
        assertNotNull(event.timestamp)
        assertEquals(1, event.version)
        assertEquals("ScheduleModified", event.eventType)
    }
    
    // Helper methods
    private fun createTestSchedule() = scheduleService.createSchedule(
        CreateScheduleRequest(
            academicYearId = UUID.randomUUID(),
            termId = UUID.randomUUID()
        )
    ).also {
        // Consume the created event to clear the queue
        scheduleCreatedEventQueue.poll(5, TimeUnit.SECONDS)
    }
    
    private fun setupScheduleCreatedEventConsumer() {
        val consumerProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "test-schedule-created-consumer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "com.opengnosis.events",
            JsonDeserializer.VALUE_DEFAULT_TYPE to ScheduleCreatedEvent::class.java.name
        )
        
        val consumerFactory = DefaultKafkaConsumerFactory<String, ScheduleCreatedEvent>(consumerProps)
        val containerProperties = ContainerProperties("schedule-events")
        containerProperties.messageListener = MessageListener<String, ScheduleCreatedEvent> { record ->
            scheduleCreatedEventQueue.add(record)
        }
        
        scheduleCreatedEventContainer = KafkaMessageListenerContainer(consumerFactory, containerProperties)
        scheduleCreatedEventContainer.start()
        ContainerTestUtils.waitForAssignment(scheduleCreatedEventContainer, 1)
    }
    
    private fun setupScheduleModifiedEventConsumer() {
        val consumerProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "test-schedule-modified-consumer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "com.opengnosis.events",
            JsonDeserializer.VALUE_DEFAULT_TYPE to ScheduleModifiedEvent::class.java.name
        )
        
        val consumerFactory = DefaultKafkaConsumerFactory<String, ScheduleModifiedEvent>(consumerProps)
        val containerProperties = ContainerProperties("schedule-events")
        containerProperties.messageListener = MessageListener<String, ScheduleModifiedEvent> { record ->
            scheduleModifiedEventQueue.add(record)
        }
        
        scheduleModifiedEventContainer = KafkaMessageListenerContainer(consumerFactory, containerProperties)
        scheduleModifiedEventContainer.start()
        ContainerTestUtils.waitForAssignment(scheduleModifiedEventContainer, 1)
    }
}
