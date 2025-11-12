package com.opengnosis.scheduler.service

import com.opengnosis.common.kafka.EventPublisher
import com.opengnosis.events.ScheduleCreatedEvent
import com.opengnosis.events.ScheduleModifiedEvent
import com.opengnosis.scheduler.dto.*
import com.opengnosis.scheduler.entity.Schedule
import com.opengnosis.scheduler.entity.ScheduleEntry
import com.opengnosis.scheduler.entity.ScheduleStatus
import com.opengnosis.scheduler.repository.ScheduleEntryRepository
import com.opengnosis.scheduler.repository.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleEntryRepository: ScheduleEntryRepository,
    private val teacherAvailabilityService: TeacherAvailabilityService,
    private val eventPublisher: EventPublisher
) {
    
    @Transactional
    fun createSchedule(request: CreateScheduleRequest): ScheduleResponse {
        val schedule = Schedule(
            academicYearId = request.academicYearId,
            termId = request.termId,
            status = ScheduleStatus.DRAFT
        )
        
        val saved = scheduleRepository.save(schedule)
        
        eventPublisher.publish(
            ScheduleCreatedEvent(
                aggregateId = saved.id,
                academicYearId = saved.academicYearId,
                termId = saved.termId,
                createdBy = UUID.randomUUID() // TODO: Get from security context
            )
        )
        
        return saved.toResponse()
    }
    
    @Transactional(readOnly = true)
    fun getSchedule(id: UUID): ScheduleResponse {
        val schedule = scheduleRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Schedule not found with id: $id") }
        
        val entries = scheduleEntryRepository.findByScheduleId(id)
        return schedule.toResponse(entries)
    }
    
    @Transactional
    fun addScheduleEntry(scheduleId: UUID, request: CreateScheduleEntryRequest): ScheduleEntryResponse {
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { IllegalArgumentException("Schedule not found with id: $scheduleId") }
        
        val dayOfWeek = DayOfWeek.valueOf(request.dayOfWeek.uppercase())
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)
        
        // Validate the entry
        val validation = validateScheduleEntry(
            scheduleId = scheduleId,
            classId = request.classId,
            teacherId = request.teacherId,
            classroomId = request.classroomId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime
        )
        
        if (!validation.valid) {
            throw ScheduleConflictException("Schedule entry validation failed", validation.conflicts)
        }
        
        val entry = ScheduleEntry(
            schedule = schedule,
            classId = request.classId,
            subjectId = request.subjectId,
            teacherId = request.teacherId,
            classroomId = request.classroomId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime
        )
        
        val saved = scheduleEntryRepository.save(entry)
        
        eventPublisher.publish(
            ScheduleModifiedEvent(
                aggregateId = scheduleId,
                scheduleEntryId = saved.id,
                classId = saved.classId,
                subjectId = saved.subjectId,
                teacherId = saved.teacherId,
                classroomId = saved.classroomId,
                dayOfWeek = saved.dayOfWeek,
                startTime = saved.startTime,
                endTime = saved.endTime,
                modifiedBy = UUID.randomUUID() // TODO: Get from security context
            )
        )
        
        return saved.toResponse()
    }
    
    @Transactional(readOnly = true)
    fun validateScheduleEntry(
        scheduleId: UUID,
        classId: UUID,
        teacherId: UUID,
        classroomId: UUID,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime
    ): ValidationResult {
        val conflicts = mutableListOf<ConflictInfo>()
        
        // Check teacher conflicts
        val teacherConflicts = scheduleEntryRepository.findTeacherConflicts(
            scheduleId, teacherId, dayOfWeek, startTime, endTime
        )
        if (teacherConflicts.isNotEmpty()) {
            conflicts.add(
                ConflictInfo(
                    type = ConflictType.TEACHER_CONFLICT,
                    message = "Teacher is already scheduled at this time",
                    conflictingEntries = teacherConflicts.map { it.toResponse() }
                )
            )
        }
        
        // Check classroom conflicts
        val classroomConflicts = scheduleEntryRepository.findClassroomConflicts(
            scheduleId, classroomId, dayOfWeek, startTime, endTime
        )
        if (classroomConflicts.isNotEmpty()) {
            conflicts.add(
                ConflictInfo(
                    type = ConflictType.CLASSROOM_CONFLICT,
                    message = "Classroom is already booked at this time",
                    conflictingEntries = classroomConflicts.map { it.toResponse() }
                )
            )
        }
        
        // Check class conflicts
        val classConflicts = scheduleEntryRepository.findClassConflicts(
            scheduleId, classId, dayOfWeek, startTime, endTime
        )
        if (classConflicts.isNotEmpty()) {
            conflicts.add(
                ConflictInfo(
                    type = ConflictType.CLASS_CONFLICT,
                    message = "Class already has a lesson scheduled at this time",
                    conflictingEntries = classConflicts.map { it.toResponse() }
                )
            )
        }
        
        // Check teacher availability
        val isAvailable = teacherAvailabilityService.isTeacherAvailable(teacherId, dayOfWeek, startTime, endTime)
        if (!isAvailable) {
            conflicts.add(
                ConflictInfo(
                    type = ConflictType.TEACHER_UNAVAILABLE,
                    message = "Teacher is not available at this time",
                    conflictingEntries = emptyList()
                )
            )
        }
        
        return ValidationResult(
            valid = conflicts.isEmpty(),
            conflicts = conflicts
        )
    }
    
    @Transactional
    fun updateScheduleEntry(scheduleId: UUID, entryId: UUID, request: CreateScheduleEntryRequest): ScheduleEntryResponse {
        val entry = scheduleEntryRepository.findById(entryId)
            .orElseThrow { IllegalArgumentException("Schedule entry not found with id: $entryId") }
        
        if (entry.schedule.id != scheduleId) {
            throw IllegalArgumentException("Schedule entry does not belong to schedule: $scheduleId")
        }
        
        val dayOfWeek = DayOfWeek.valueOf(request.dayOfWeek.uppercase())
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)
        
        // Validate the updated entry (excluding the current entry from conflict checks)
        scheduleEntryRepository.delete(entry)
        
        val validation = validateScheduleEntry(
            scheduleId = scheduleId,
            classId = request.classId,
            teacherId = request.teacherId,
            classroomId = request.classroomId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime
        )
        
        if (!validation.valid) {
            // Restore the entry if validation fails
            scheduleEntryRepository.save(entry)
            throw ScheduleConflictException("Schedule entry validation failed", validation.conflicts)
        }
        
        val updated = entry.copy(
            classId = request.classId,
            subjectId = request.subjectId,
            teacherId = request.teacherId,
            classroomId = request.classroomId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime
        )
        
        val saved = scheduleEntryRepository.save(updated)
        
        eventPublisher.publish(
            ScheduleModifiedEvent(
                aggregateId = scheduleId,
                scheduleEntryId = saved.id,
                classId = saved.classId,
                subjectId = saved.subjectId,
                teacherId = saved.teacherId,
                classroomId = saved.classroomId,
                dayOfWeek = saved.dayOfWeek,
                startTime = saved.startTime,
                endTime = saved.endTime,
                modifiedBy = UUID.randomUUID() // TODO: Get from security context
            )
        )
        
        return saved.toResponse()
    }
    
    @Transactional
    fun deleteScheduleEntry(scheduleId: UUID, entryId: UUID) {
        val entry = scheduleEntryRepository.findById(entryId)
            .orElseThrow { IllegalArgumentException("Schedule entry not found with id: $entryId") }
        
        if (entry.schedule.id != scheduleId) {
            throw IllegalArgumentException("Schedule entry does not belong to schedule: $scheduleId")
        }
        
        val entryData = entry.copy() // Save data before deletion
        scheduleEntryRepository.delete(entry)
        
        eventPublisher.publish(
            ScheduleModifiedEvent(
                aggregateId = scheduleId,
                scheduleEntryId = entryData.id,
                classId = entryData.classId,
                subjectId = entryData.subjectId,
                teacherId = entryData.teacherId,
                classroomId = entryData.classroomId,
                dayOfWeek = entryData.dayOfWeek,
                startTime = entryData.startTime,
                endTime = entryData.endTime,
                modifiedBy = UUID.randomUUID() // TODO: Get from security context
            )
        )
    }
    
    @Transactional
    fun updateScheduleStatus(scheduleId: UUID, status: ScheduleStatus): ScheduleResponse {
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { IllegalArgumentException("Schedule not found with id: $scheduleId") }
        
        val updated = schedule.copy(status = status)
        val saved = scheduleRepository.save(updated)
        
        val entries = scheduleEntryRepository.findByScheduleId(scheduleId)
        return saved.toResponse(entries)
    }
    
    private fun Schedule.toResponse(entries: List<ScheduleEntry> = emptyList()) = ScheduleResponse(
        id = id,
        academicYearId = academicYearId,
        termId = termId,
        status = status,
        entries = entries.map { it.toResponse() }
    )
    
    private fun ScheduleEntry.toResponse() = ScheduleEntryResponse(
        id = id,
        scheduleId = schedule.id,
        classId = classId,
        subjectId = subjectId,
        teacherId = teacherId,
        classroomId = classroomId,
        dayOfWeek = dayOfWeek.name,
        startTime = startTime.toString(),
        endTime = endTime.toString()
    )
}

class ScheduleConflictException(
    message: String,
    val conflicts: List<ConflictInfo>
) : RuntimeException(message)
