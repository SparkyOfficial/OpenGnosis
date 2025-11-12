package com.opengnosis.scheduler.integration

import com.opengnosis.scheduler.dto.CreateScheduleEntryRequest
import com.opengnosis.scheduler.dto.CreateScheduleRequest
import com.opengnosis.scheduler.entity.Classroom
import com.opengnosis.scheduler.entity.TeacherAvailability
import com.opengnosis.scheduler.repository.ClassroomRepository
import com.opengnosis.scheduler.repository.ScheduleEntryRepository
import com.opengnosis.scheduler.repository.TeacherAvailabilityRepository
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
class ScheduleQueryTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var scheduleService: ScheduleService
    
    @Autowired
    private lateinit var scheduleEntryRepository: ScheduleEntryRepository
    
    @Autowired
    private lateinit var classroomRepository: ClassroomRepository
    
    @Autowired
    private lateinit var teacherAvailabilityRepository: TeacherAvailabilityRepository
    
    private lateinit var testClassroomId: UUID
    private lateinit var testTeacherId: UUID
    private lateinit var testClassId: UUID
    
    @BeforeEach
    fun setup() {
        testClassroomId = UUID.randomUUID()
        testTeacherId = UUID.randomUUID()
        testClassId = UUID.randomUUID()
        
        // Create test classroom
        classroomRepository.save(
            Classroom(
                id = testClassroomId,
                schoolId = UUID.randomUUID(),
                name = "Room 101",
                capacity = 30
            )
        )
        
        // Create teacher availability
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
    fun `should query schedule by id`() {
        // Given
        val schedule = createTestSchedule()
        
        // Add some entries
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = testClassId,
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "MONDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = testClassId,
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "TUESDAY",
                startTime = "10:00",
                endTime = "11:00"
            )
        )
        
        // When
        val retrieved = scheduleService.getSchedule(schedule.id)
        
        // Then
        assertEquals(schedule.id, retrieved.id)
        assertEquals(2, retrieved.entries.size)
    }
    
    @Test
    fun `should query schedule entries by class id`() {
        // Given
        val schedule = createTestSchedule()
        val class1Id = UUID.randomUUID()
        val class2Id = UUID.randomUUID()
        
        // Add entries for different classes
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = class1Id,
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "MONDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = class1Id,
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "TUESDAY",
                startTime = "10:00",
                endTime = "11:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = class2Id,
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "WEDNESDAY",
                startTime = "11:00",
                endTime = "12:00"
            )
        )
        
        // When
        val class1Entries = scheduleEntryRepository.findByClassId(class1Id)
        val class2Entries = scheduleEntryRepository.findByClassId(class2Id)
        
        // Then
        assertEquals(2, class1Entries.size)
        assertEquals(1, class2Entries.size)
        assertTrue(class1Entries.all { it.classId == class1Id })
        assertTrue(class2Entries.all { it.classId == class2Id })
    }
    
    @Test
    fun `should query schedule entries by teacher id`() {
        // Given
        val schedule = createTestSchedule()
        val teacher1Id = UUID.randomUUID()
        val teacher2Id = UUID.randomUUID()
        
        // Create availability for second teacher
        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)
        days.forEach { day ->
            teacherAvailabilityRepository.save(
                TeacherAvailability(
                    teacherId = teacher2Id,
                    dayOfWeek = day,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0)
                )
            )
        }
        
        // Add entries for different teachers
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = teacher1Id,
                classroomId = testClassroomId,
                dayOfWeek = "MONDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = teacher2Id,
                classroomId = testClassroomId,
                dayOfWeek = "TUESDAY",
                startTime = "10:00",
                endTime = "11:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = teacher1Id,
                classroomId = testClassroomId,
                dayOfWeek = "WEDNESDAY",
                startTime = "11:00",
                endTime = "12:00"
            )
        )
        
        // When
        val teacher1Entries = scheduleEntryRepository.findByTeacherId(teacher1Id)
        val teacher2Entries = scheduleEntryRepository.findByTeacherId(teacher2Id)
        
        // Then
        assertEquals(2, teacher1Entries.size)
        assertEquals(1, teacher2Entries.size)
        assertTrue(teacher1Entries.all { it.teacherId == teacher1Id })
        assertTrue(teacher2Entries.all { it.teacherId == teacher2Id })
    }
    
    @Test
    fun `should query schedule entries by classroom id`() {
        // Given
        val schedule = createTestSchedule()
        val classroom1Id = UUID.randomUUID()
        val classroom2Id = UUID.randomUUID()
        
        // Create additional classrooms
        classroomRepository.save(
            Classroom(
                id = classroom1Id,
                schoolId = UUID.randomUUID(),
                name = "Room 201",
                capacity = 25
            )
        )
        
        classroomRepository.save(
            Classroom(
                id = classroom2Id,
                schoolId = UUID.randomUUID(),
                name = "Room 202",
                capacity = 20
            )
        )
        
        // Add entries for different classrooms
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = classroom1Id,
                dayOfWeek = "MONDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = classroom2Id,
                dayOfWeek = "TUESDAY",
                startTime = "10:00",
                endTime = "11:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = classroom1Id,
                dayOfWeek = "WEDNESDAY",
                startTime = "11:00",
                endTime = "12:00"
            )
        )
        
        // When
        val classroom1Entries = scheduleEntryRepository.findByClassroomId(classroom1Id)
        val classroom2Entries = scheduleEntryRepository.findByClassroomId(classroom2Id)
        
        // Then
        assertEquals(2, classroom1Entries.size)
        assertEquals(1, classroom2Entries.size)
        assertTrue(classroom1Entries.all { it.classroomId == classroom1Id })
        assertTrue(classroom2Entries.all { it.classroomId == classroom2Id })
    }
    
    @Test
    fun `should query schedule entries by schedule id`() {
        // Given
        val schedule1 = createTestSchedule()
        val schedule2 = createTestSchedule()
        
        // Add entries to both schedules
        scheduleService.addScheduleEntry(
            schedule1.id,
            CreateScheduleEntryRequest(
                classId = testClassId,
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "MONDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule1.id,
            CreateScheduleEntryRequest(
                classId = testClassId,
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "TUESDAY",
                startTime = "10:00",
                endTime = "11:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule2.id,
            CreateScheduleEntryRequest(
                classId = testClassId,
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "WEDNESDAY",
                startTime = "11:00",
                endTime = "12:00"
            )
        )
        
        // When
        val schedule1Entries = scheduleEntryRepository.findByScheduleId(schedule1.id)
        val schedule2Entries = scheduleEntryRepository.findByScheduleId(schedule2.id)
        
        // Then
        assertEquals(2, schedule1Entries.size)
        assertEquals(1, schedule2Entries.size)
    }
    
    @Test
    fun `should filter entries by multiple criteria`() {
        // Given
        val schedule = createTestSchedule()
        val specificClassId = UUID.randomUUID()
        val specificTeacherId = UUID.randomUUID()
        
        // Create availability for specific teacher
        teacherAvailabilityRepository.save(
            TeacherAvailability(
                teacherId = specificTeacherId,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(16, 0)
            )
        )
        
        // Add various entries
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = specificClassId,
                subjectId = UUID.randomUUID(),
                teacherId = specificTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "MONDAY",
                startTime = "09:00",
                endTime = "10:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = UUID.randomUUID(),
                subjectId = UUID.randomUUID(),
                teacherId = specificTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "MONDAY",
                startTime = "10:00",
                endTime = "11:00"
            )
        )
        
        scheduleService.addScheduleEntry(
            schedule.id,
            CreateScheduleEntryRequest(
                classId = specificClassId,
                subjectId = UUID.randomUUID(),
                teacherId = testTeacherId,
                classroomId = testClassroomId,
                dayOfWeek = "MONDAY",
                startTime = "11:00",
                endTime = "12:00"
            )
        )
        
        // When
        val classEntries = scheduleEntryRepository.findByClassId(specificClassId)
        val teacherEntries = scheduleEntryRepository.findByTeacherId(specificTeacherId)
        
        // Then
        assertEquals(2, classEntries.size)
        assertEquals(2, teacherEntries.size)
        
        // Find entries matching both criteria
        val matchingBoth = classEntries.filter { it.teacherId == specificTeacherId }
        assertEquals(1, matchingBoth.size)
    }
    
    @Test
    fun `should return empty list for non-existent queries`() {
        // Given
        val nonExistentId = UUID.randomUUID()
        
        // When
        val entries = scheduleEntryRepository.findByClassId(nonExistentId)
        
        // Then
        assertTrue(entries.isEmpty())
    }
    
    private fun createTestSchedule() = scheduleService.createSchedule(
        CreateScheduleRequest(
            academicYearId = UUID.randomUUID(),
            termId = UUID.randomUUID()
        )
    )
}
