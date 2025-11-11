package com.opengnosis.scheduler.service

import com.opengnosis.scheduler.dto.TeacherAvailabilityRequest
import com.opengnosis.scheduler.dto.TeacherAvailabilityResponse
import com.opengnosis.scheduler.entity.TeacherAvailability
import com.opengnosis.scheduler.repository.TeacherAvailabilityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Service
class TeacherAvailabilityService(
    private val teacherAvailabilityRepository: TeacherAvailabilityRepository
) {
    
    @Transactional
    fun createAvailability(request: TeacherAvailabilityRequest): TeacherAvailabilityResponse {
        val availability = TeacherAvailability(
            teacherId = request.teacherId,
            dayOfWeek = DayOfWeek.valueOf(request.dayOfWeek.uppercase()),
            startTime = LocalTime.parse(request.startTime),
            endTime = LocalTime.parse(request.endTime),
            available = request.available
        )
        
        val saved = teacherAvailabilityRepository.save(availability)
        return saved.toResponse()
    }
    
    @Transactional(readOnly = true)
    fun getTeacherAvailability(teacherId: UUID): List<TeacherAvailabilityResponse> {
        return teacherAvailabilityRepository.findByTeacherId(teacherId)
            .map { it.toResponse() }
    }
    
    @Transactional(readOnly = true)
    fun getTeacherAvailabilityForDay(teacherId: UUID, dayOfWeek: DayOfWeek): List<TeacherAvailabilityResponse> {
        return teacherAvailabilityRepository.findByTeacherIdAndDayOfWeek(teacherId, dayOfWeek)
            .map { it.toResponse() }
    }
    
    @Transactional
    fun deleteAvailability(id: UUID) {
        if (!teacherAvailabilityRepository.existsById(id)) {
            throw IllegalArgumentException("Teacher availability not found with id: $id")
        }
        teacherAvailabilityRepository.deleteById(id)
    }
    
    @Transactional(readOnly = true)
    fun isTeacherAvailable(teacherId: UUID, dayOfWeek: DayOfWeek, startTime: LocalTime, endTime: LocalTime): Boolean {
        val availabilities = teacherAvailabilityRepository.findByTeacherIdAndDayOfWeek(teacherId, dayOfWeek)
        
        return availabilities.any { availability ->
            availability.available &&
            availability.startTime <= startTime &&
            availability.endTime >= endTime
        }
    }
    
    private fun TeacherAvailability.toResponse() = TeacherAvailabilityResponse(
        id = id,
        teacherId = teacherId,
        dayOfWeek = dayOfWeek.name,
        startTime = startTime.toString(),
        endTime = endTime.toString(),
        available = available
    )
}
