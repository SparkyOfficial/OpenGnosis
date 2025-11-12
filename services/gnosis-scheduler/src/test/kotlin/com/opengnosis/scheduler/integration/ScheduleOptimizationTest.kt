package com.opengnosis.scheduler.integration

import com.opengnosis.scheduler.dto.CreateScheduleEntryRequest
import com.opengnosis.scheduler.dto.CreateScheduleRequest
import com.opengnosis.scheduler.entity.Classroom
import com.opengnosis.scheduler.entity.TeacherAvailability
import com.opengnosis.scheduler.repository.ClassroomRepository
import com.opengnosis.scheduler.repository.ScheduleEntryRepository
import com.opengnosis.scheduler.repository.TeacherAvailabilityRepository
import com.opengnosis.scheduler.service.OptimizationService
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
class ScheduleOptimizationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var scheduleService: ScheduleService
    
    @Autowired
    private lateinit var optimizationService: OptimizationService
    
    @Autowired
    private lateinit var scheduleEntryRepository: ScheduleEntryRepository
    
    @Autowired
    private lateinit var classroomRepository: ClassroomRepository
    
    @Autowired
    private lateinit var teacherAvailabilityRepository: TeacherAvailabilityRepository
    
    private lateinit var classroom1Id: UUID
    private lateinit var classroom2Id: UUID
    private lateinit var teacher1Id: UUID
    private lateinit var teacher2Id: UUID
    
    @BeforeEach
    fun setup() {
        // Create test classrooms
        classroom1Id = UUID.randomUUID()
        classroom2Id = UUID.randomUUID()
        
        classroomRepository.save(
            Classroom(
                id = classroom1Id,
                schoolId = UUID.randomUUID(),
                name = "Room 101",
                capacity = 30
            )
        )
        
        classroomRepository.save(
            Classroom(
                id = classroom2Id,
                schoolId = UUID.randomUUID(),
                name = "Room 102",
                capacity = 25
            )
        )
        
        // Create teacher availabilities
        teacher1Id = UUID.randomUUID()
        teacher2Id = UUID.randomUUID()
        
        val days = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
        
        days.forEach { day ->
            teacherAvailabilityRepository.save(
                TeacherAvailability(
                    teacherId = teacher1Id,
                    dayOfWeek = day,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0)
                )
            )
            
            teacherAvailabilityRepository.save(
                TeacherAvailability(
                    teacherId = teacher2Id,
                    dayOfWeek = day,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0)
                )
            )
        }
    }
    
    @Test
    fun `should optimize schedule with sample data`() {
        // Given
        val schedule = createTestSchedule()
        
        // Add multiple schedule entries
        val class1Id = UUID.randomUUID()
        val class2Id = UUID.randomUUID()
        val subject1Id = UUID.randomUUID()
        val subject2Id = UUID.randomUUID()
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = class1Id,
                subjectId = subject1Id,
                teacherId = teacher1Id,
                classroomId = classroom1Id,
                dayOfWeek = "MONDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = class2Id,
                subjectId = subject2Id,
                teacherId = teacher2Id,
                classroomId = classroom2Id,
                dayOfWeek = "MONDAY",
                startTime = "10:00",
                endTime = "11:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = class1Id,
                subjectId = subject2Id,
                teacherId = teacher2Id,
                classroomId = classroom1Id,
                dayOfWeek = "TUESDAY",
                startTime = "11:00",
                endTime = "12:00"
            )
        )
        
        // When
        val jobId = optimizationService.optimizeSchedule(schedule.id)
        
        // Then
        assertNotNull(jobId)
        assertEquals(schedule.id, jobId)
        
        // Wait a bit for optimization to process
        Thread.sleep(2000)
        
        // Get optimization result
        val result = optimizationService.getOptimizationResult(schedule.id)
        
        // Verify result contains entries
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals(3, result.size)
    }
    
    @Test
    fun `should retrieve optimization result`() {
        // Given
        val schedule = createTestSchedule()
        
        // Add a simple entry
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = teacher1Id,
                classroomId = classroom1Id,
                dayOfWeek = "WEDNESDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        // Start optimization
        optimizationService.optimizeSchedule(schedule.id)
        
        // When
        val result = optimizationService.getOptimizationResult(schedule.id)
        
        // Then
        assertNotNull(result)
        assertFalse(result.isEmpty())
    }
    
    @Test
    fun `should apply optimized schedule`() {
        // Given
        val schedule = createTestSchedule()
        val class1Id = UUID.randomUUID()
        val subject1Id = UUID.randomUUID()
        
        // Add initial entries
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = class1Id,
                subjectId = subject1Id,
                teacherId = teacher1Id,
                classroomId = classroom1Id,
                dayOfWeek = "THURSDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = class1Id,
                subjectId = subject1Id,
                teacherId = teacher1Id,
                classroomId = classroom1Id,
                dayOfWeek = "THURSDAY",
                startTime = "11:00",
                endTime = "12:00"
            )
        )
        
        // Start optimization
        optimizationService.optimizeSchedule(schedule.id)
        Thread.sleep(2000)
        
        // When
        optimizationService.applyOptimizedSchedule(schedule.id)
        
        // Then
        val entries = scheduleEntryRepository.findByScheduleId(schedule.id)
        assertNotNull(entries)
        assertEquals(2, entries.size)
    }
    
    @Test
    fun `should handle optimization with multiple teachers and classrooms`() {
        // Given
        val schedule = createTestSchedule()
        
        // Create a more complex schedule with multiple classes, teachers, and classrooms
        val classes = (1..3).map { UUID.randomUUID() }
        val subjects = (1..3).map { UUID.randomUUID() }
        
        // Add entries for different combinations
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = classes[0],
                subjectId = subjects[0],
                teacherId = teacher1Id,
                classroomId = classroom1Id,
                dayOfWeek = "MONDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = classes[1],
                subjectId = subjects[1],
                teacherId = teacher2Id,
                classroomId = classroom2Id,
                dayOfWeek = "MONDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = classes[2],
                subjectId = subjects[2],
                teacherId = teacher1Id,
                classroomId = classroom2Id,
                dayOfWeek = "TUESDAY",
                startTime = "10:00",
                endTime = "11:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = classes[0],
                subjectId = subjects[1],
                teacherId = teacher2Id,
                classroomId = classroom1Id,
                dayOfWeek = "WEDNESDAY",
                startTime = "11:00",
                endTime = "12:00"
            )
        )
        
        // When
        val jobId = optimizationService.optimizeSchedule(schedule.id)
        
        // Then
        assertNotNull(jobId)
        
        // Wait for optimization
        Thread.sleep(2000)
        
        val result = optimizationService.getOptimizationResult(schedule.id)
        assertEquals(4, result.size)
        
        // Verify no conflicts in optimized schedule
        result.forEach { entry ->
            assertNotNull(entry.classroomId)
            assertNotNull(entry.teacherId)
            assertNotNull(entry.dayOfWeek)
            assertNotNull(entry.startTime)
            assertNotNull(entry.endTime)
        }
    }
    
    @Test
    fun `should handle empty schedule optimization`() {
        // Given
        val schedule = createTestSchedule()
        
        // When - optimize empty schedule
        val jobId = optimizationService.optimizeSchedule(schedule.id)
        
        // Then
        assertNotNull(jobId)
        
        val result = optimizationService.getOptimizationResult(schedule.id)
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }
    
    private fun createTestSchedule() = scheduleService.createSchedule(
        CreateScheduleRequest(
            academicYearId = UUID.randomUUID(),
            termId = UUID.randomUUID()
        )
    )
}
