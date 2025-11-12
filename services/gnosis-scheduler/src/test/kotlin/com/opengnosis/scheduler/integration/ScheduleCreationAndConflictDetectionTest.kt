package com.opengnosis.scheduler.integration

import com.opengnosis.scheduler.dto.CreateScheduleEntryRequest
import com.opengnosis.scheduler.dto.CreateScheduleRequest
import com.opengnosis.scheduler.entity.Classroom
import com.opengnosis.scheduler.entity.ScheduleStatus
import com.opengnosis.scheduler.entity.TeacherAvailability
import com.opengnosis.scheduler.repository.ClassroomRepository
import com.opengnosis.scheduler.repository.ScheduleEntryRepository
import com.opengnosis.scheduler.repository.ScheduleRepository
import com.opengnosis.scheduler.repository.TeacherAvailabilityRepository
import com.opengnosis.scheduler.service.ScheduleConflictException
import com.opengnosis.scheduler.service.ScheduleService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Transactional
class ScheduleCreationAndConflictDetectionTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var scheduleService: ScheduleService
    
    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository
    
    @Autowired
    private lateinit var scheduleEntryRepository: ScheduleEntryRepository
    
    @Autowired
    private lateinit var classroomRepository: ClassroomRepository
    
    @Autowired
    private lateinit var teacherAvailabilityRepository: TeacherAvailabilityRepository
    
    private lateinit var testClassroomId: UUID
    private lateinit var testTeacherId: UUID
    private lateinit var testClassId: UUID
    private lateinit var testSubjectId: UUID
    
    @BeforeEach
    fun setup() {
        // Create test data
        testClassroomId = UUID.randomUUID()
        testTeacherId = UUID.randomUUID()
        testClassId = UUID.randomUUID()
        testSubjectId = UUID.randomUUID()
        
        // Create a test classroom
        val classroom = Classroom(
            id = testClassroomId,
            schoolId = UUID.randomUUID(),
            name = "Room 101",
            capacity = 30
        )
        classroomRepository.save(classroom)
        
        // Create teacher availability (Monday-Friday, 8:00-16:00)
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
    fun `should create schedule successfully`() {
        // Given
        val academicYearId = UUID.randomUUID()
        val termId = UUID.randomUUID()
        val request = CreateScheduleRequest(
            academicYearId = academicYearId,
            termId = termId
        )
        
        // When
        val response = scheduleService.createSchedule(request)
        
        // Then
        assertNotNull(response.id)
        assertEquals(academicYearId, response.academicYearId)
        assertEquals(termId, response.termId)
        assertEquals(ScheduleStatus.DRAFT, response.status)
        assertTrue(response.entries.isEmpty())
        
        // Verify in database
        val savedSchedule = scheduleRepository.findById(response.id)
        assertTrue(savedSchedule.isPresent)
        assertEquals(academicYearId, savedSchedule.get().academicYearId)
    }
    
    @Test
    fun `should add schedule entry successfully`() {
        // Given
        val schedule = createTestSchedule()
        val entryRequest = CreateScheduleEntryRequest(
            classId = testClassId,
            subjectId = testSubjectId,
            teacherId = testTeacherId,
            classroomId = testClassroomId,
            dayOfWeek = "MONDAY",
            startTime = "09:00",
            endTime = "10:00"
        )
        
        // When
        val response = scheduleService.addScheduleEntry(schedule.id, entryRequest)
        
        // Then
        assertNotNull(response.id)
        assertEquals(schedule.id, response.scheduleId)
        assertEquals(testClassId, response.classId)
        assertEquals(testSubjectId, response.subjectId)
        assertEquals(testTeacherId, response.teacherId)
        assertEquals(testClassroomId, response.classroomId)
        assertEquals("MONDAY", response.dayOfWeek)
        assertEquals("09:00", response.startTime)
        assertEquals("10:00", response.endTime)
        
        // Verify in database
        val entries = scheduleEntryRepository.findByScheduleId(schedule.id)
        assertEquals(1, entries.size)
        assertEquals(testClassId, entries[0].classId)
    }
    
    @Test
    fun `should detect teacher conflict`() {
        // Given
        val schedule = createTestSchedule()
        
        // Create first entry
        val firstEntry = CreateScheduleEntryRequest(
            classId = testClassId,
            subjectId = testSubjectId,
            teacherId = testTeacherId,
            classroomId = testClassroomId,
            dayOfWeek = "MONDAY",
            startTime = "09:00",
            endTime = "10:00"
        )
        scheduleService.addScheduleEntry(schedule.id, firstEntry)
        
        // Try to create conflicting entry with same teacher
        val conflictingEntry = CreateScheduleEntryRequest(
            classId = UUID.randomUUID(), // Different class
            subjectId = UUID.randomUUID(),
            teacherId = testTeacherId, // Same teacher
            classroomId = UUID.randomUUID(), // Different classroom
            dayOfWeek = "MONDAY",
            startTime = "09:30", // Overlapping time
            endTime = "10:30"
        )
        
        // When & Then
        val exception = assertThrows(ScheduleConflictException::class.java) {
            scheduleService.addScheduleEntry(schedule.id, conflictingEntry)
        }
        
        assertTrue(exception.conflicts.any { it.type.name == "TEACHER_CONFLICT" })
        assertTrue(exception.message!!.contains("validation failed"))
    }
    
    @Test
    fun `should detect classroom conflict`() {
        // Given
        val schedule = createTestSchedule()
        
        // Create first entry
        val firstEntry = CreateScheduleEntryRequest(
            classId = testClassId,
            subjectId = testSubjectId,
            teacherId = testTeacherId,
            classroomId = testClassroomId,
            dayOfWeek = "TUESDAY",
            startTime = "10:00",
            endTime = "11:00"
        )
        scheduleService.addScheduleEntry(schedule.id, firstEntry)
        
        // Try to create conflicting entry with same classroom
        val conflictingEntry = CreateScheduleEntryRequest(
            classId = UUID.randomUUID(),
            subjectId = UUID.randomUUID(),
            teacherId = UUID.randomUUID(), // Different teacher
            classroomId = testClassroomId, // Same classroom
            dayOfWeek = "TUESDAY",
            startTime = "10:30", // Overlapping time
            endTime = "11:30"
        )
        
        // When & Then
        val exception = assertThrows(ScheduleConflictException::class.java) {
            scheduleService.addScheduleEntry(schedule.id, conflictingEntry)
        }
        
        assertTrue(exception.conflicts.any { it.type.name == "CLASSROOM_CONFLICT" })
    }
    
    @Test
    fun `should detect class conflict`() {
        // Given
        val schedule = createTestSchedule()
        
        // Create first entry
        val firstEntry = CreateScheduleEntryRequest(
            classId = testClassId,
            subjectId = testSubjectId,
            teacherId = testTeacherId,
            classroomId = testClassroomId,
            dayOfWeek = "WEDNESDAY",
            startTime = "11:00",
            endTime = "12:00"
        )
        scheduleService.addScheduleEntry(schedule.id, firstEntry)
        
        // Try to create conflicting entry with same class
        val conflictingEntry = CreateScheduleEntryRequest(
            classId = testClassId, // Same class
            subjectId = UUID.randomUUID(),
            teacherId = UUID.randomUUID(), // Different teacher
            classroomId = UUID.randomUUID(), // Different classroom
            dayOfWeek = "WEDNESDAY",
            startTime = "11:30", // Overlapping time
            endTime = "12:30"
        )
        
        // When & Then
        val exception = assertThrows(ScheduleConflictException::class.java) {
            scheduleService.addScheduleEntry(schedule.id, conflictingEntry)
        }
        
        assertTrue(exception.conflicts.any { it.type.name == "CLASS_CONFLICT" })
    }
    
    @Test
    fun `should detect teacher unavailability`() {
        // Given
        val schedule = createTestSchedule()
        
        // Try to create entry outside teacher availability (17:00-18:00)
        val entryRequest = CreateScheduleEntryRequest(
            classId = testClassId,
            subjectId = testSubjectId,
            teacherId = testTeacherId,
            classroomId = testClassroomId,
            dayOfWeek = "THURSDAY",
            startTime = "17:00", // Outside availability
            endTime = "18:00"
        )
        
        // When & Then
        val exception = assertThrows(ScheduleConflictException::class.java) {
            scheduleService.addScheduleEntry(schedule.id, entryRequest)
        }
        
        assertTrue(exception.conflicts.any { it.type.name == "TEACHER_UNAVAILABLE" })
    }
    
    @Test
    fun `should allow non-overlapping entries`() {
        // Given
        val schedule = createTestSchedule()
        
        // Create first entry
        val firstEntry = CreateScheduleEntryRequest(
            classId = testClassId,
            subjectId = testSubjectId,
            teacherId = testTeacherId,
            classroomId = testClassroomId,
            dayOfWeek = "FRIDAY",
            startTime = "09:00",
            endTime = "10:00"
        )
        scheduleService.addScheduleEntry(schedule.id, firstEntry)
        
        // Create non-overlapping entry with same resources
        val secondEntry = CreateScheduleEntryRequest(
            classId = testClassId,
            subjectId = testSubjectId,
            teacherId = testTeacherId,
            classroomId = testClassroomId,
            dayOfWeek = "FRIDAY",
            startTime = "10:00", // Starts when first ends
            endTime = "11:00"
        )
        
        // When
        val response = scheduleService.addScheduleEntry(schedule.id, secondEntry)
        
        // Then
        assertNotNull(response.id)
        
        // Verify both entries exist
        val entries = scheduleEntryRepository.findByScheduleId(schedule.id)
        assertEquals(2, entries.size)
    }
    
    @Test
    fun `should update schedule entry successfully`() {
        // Given
        val schedule = createTestSchedule()
        val originalEntry = scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = testClassId,
                subjectId = testSubjectId,
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "MONDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        // When
        val updateRequest = CreateScheduleEntryRequest(
            classId = testClassId,
            subjectId = testSubjectId,
            teacherId = testTeacherId,
            classroomId = testClassroomId,
            dayOfWeek = "MONDAY",
            startTime = "11:00", // Changed time
            endTime = "12:00"
        )
        val updated = scheduleService.updateScheduleEntry(schedule.id, originalEntry.id, updateRequest)
        
        // Then
        assertEquals(originalEntry.id, updated.id)
        assertEquals("11:00", updated.startTime)
        assertEquals("12:00", updated.endTime)
    }
    
    @Test
    fun `should delete schedule entry successfully`() {
        // Given
        val schedule = createTestSchedule()
        val entry = scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = testClassId,
                subjectId = testSubjectId,
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "MONDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        // When
        scheduleService.deleteScheduleEntry(schedule.id, entry.id)
        
        // Then
        val entries = scheduleEntryRepository.findByScheduleId(schedule.id)
        assertTrue(entries.isEmpty())
    }
    
    @Test
    fun `should update schedule status`() {
        // Given
        val schedule = createTestSchedule()
        assertEquals(ScheduleStatus.DRAFT, schedule.status)
        
        // When
        val updated = scheduleService.updateScheduleStatus(schedule.id, ScheduleStatus.ACTIVE)
        
        // Then
        assertEquals(ScheduleStatus.ACTIVE, updated.status)
        
        // Verify in database
        val savedSchedule = scheduleRepository.findById(schedule.id).get()
        assertEquals(ScheduleStatus.ACTIVE, savedSchedule.status)
    }
    
    private fun createTestSchedule() = scheduleService.createSchedule(
        CreateScheduleRequest(
            academicYearId = UUID.randomUUID(),
            termId = UUID.randomUUID()
        )
    )
}
