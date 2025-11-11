package com.opengnosis.scheduler.service

import com.opengnosis.scheduler.dto.ScheduleEntryResponse
import com.opengnosis.scheduler.entity.ScheduleEntry
import com.opengnosis.scheduler.repository.ScheduleEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ScheduleQueryService(
    private val scheduleEntryRepository: ScheduleEntryRepository
) {
    
    @Transactional(readOnly = true)
    fun getScheduleByClass(classId: UUID): List<ScheduleEntryResponse> {
        return scheduleEntryRepository.findByClassId(classId)
            .map { it.toResponse() }
    }
    
    @Transactional(readOnly = true)
    fun getScheduleByTeacher(teacherId: UUID): List<ScheduleEntryResponse> {
        return scheduleEntryRepository.findByTeacherId(teacherId)
            .map { it.toResponse() }
    }
    
    @Transactional(readOnly = true)
    fun getScheduleByClassroom(classroomId: UUID): List<ScheduleEntryResponse> {
        return scheduleEntryRepository.findByClassroomId(classroomId)
            .map { it.toResponse() }
    }
    
    @Transactional(readOnly = true)
    fun getScheduleEntry(entryId: UUID): ScheduleEntryResponse {
        val entry = scheduleEntryRepository.findById(entryId)
            .orElseThrow { IllegalArgumentException("Schedule entry not found with id: $entryId") }
        return entry.toResponse()
    }
    
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
