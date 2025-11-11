package com.opengnosis.scheduler.service

import com.opengnosis.scheduler.dto.ScheduleEntryResponse
import com.opengnosis.scheduler.entity.ScheduleEntry
import com.opengnosis.scheduler.optaplanner.SchedulePlanningEntity
import com.opengnosis.scheduler.optaplanner.ScheduleSolution
import com.opengnosis.scheduler.optaplanner.TeacherAvailabilityConstraint
import com.opengnosis.scheduler.optaplanner.TimeSlot
import com.opengnosis.scheduler.repository.ClassroomRepository
import com.opengnosis.scheduler.repository.ScheduleEntryRepository
import com.opengnosis.scheduler.repository.ScheduleRepository
import com.opengnosis.scheduler.repository.TeacherAvailabilityRepository
import org.optaplanner.core.api.solver.SolverJob
import org.optaplanner.core.api.solver.SolverManager
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.ExecutionException

@Service
class OptimizationService(
    private val solverManager: SolverManager<ScheduleSolution, UUID>,
    private val scheduleRepository: ScheduleRepository,
    private val scheduleEntryRepository: ScheduleEntryRepository,
    private val classroomRepository: ClassroomRepository,
    private val teacherAvailabilityRepository: TeacherAvailabilityRepository
) {
    
    fun optimizeSchedule(scheduleId: UUID): UUID {
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { IllegalArgumentException("Schedule not found with id: $scheduleId") }
        
        // Get existing entries
        val existingEntries = scheduleEntryRepository.findByScheduleId(scheduleId)
        
        // Get available classrooms
        val classrooms = classroomRepository.findAll().map { it.id }
        
        // Generate time slots (example: Monday-Friday, 8:00-16:00, 1-hour slots)
        val timeSlots = generateTimeSlots()
        
        // Get teacher availabilities
        val teacherAvailabilities = existingEntries
            .map { it.teacherId }
            .distinct()
            .map { teacherId ->
                val availabilities = teacherAvailabilityRepository.findByTeacherId(teacherId)
                TeacherAvailabilityConstraint(
                    teacherId = teacherId,
                    availableTimeSlots = availabilities.map { av ->
                        TimeSlot(av.dayOfWeek, av.startTime, av.endTime)
                    }
                )
            }
        
        // Convert existing entries to planning entities
        val planningEntities = existingEntries.map { entry ->
            SchedulePlanningEntity(
                id = entry.id,
                classId = entry.classId,
                subjectId = entry.subjectId,
                teacherId = entry.teacherId,
                classroomId = entry.classroomId,
                timeSlot = TimeSlot(entry.dayOfWeek, entry.startTime, entry.endTime)
            )
        }
        
        val problem = ScheduleSolution(
            classrooms = classrooms,
            timeSlots = timeSlots,
            teacherAvailabilities = teacherAvailabilities,
            scheduleEntries = planningEntities
        )
        
        // Start solving asynchronously
        val solverJob: SolverJob<ScheduleSolution, UUID> = solverManager.solve(scheduleId, problem)
        
        return scheduleId
    }
    
    fun getOptimizationResult(scheduleId: UUID): List<ScheduleEntryResponse> {
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { IllegalArgumentException("Schedule not found with id: $scheduleId") }
        
        try {
            val solverJob = solverManager.getSolverJob(scheduleId)
            if (solverJob != null) {
                val solution = solverJob.finalBestSolution
                
                if (solution != null) {
                    // Convert optimized solution back to schedule entries
                    return solution.scheduleEntries
                        .filter { it.classroomId != null && it.timeSlot != null }
                        .map { entity ->
                            ScheduleEntryResponse(
                                id = entity.id,
                                scheduleId = scheduleId,
                                classId = entity.classId,
                                subjectId = entity.subjectId,
                                teacherId = entity.teacherId,
                                classroomId = entity.classroomId!!,
                                dayOfWeek = entity.timeSlot!!.dayOfWeek.name,
                                startTime = entity.timeSlot!!.startTime.toString(),
                                endTime = entity.timeSlot!!.endTime.toString()
                            )
                        }
                }
            }
        } catch (e: ExecutionException) {
            throw RuntimeException("Optimization failed", e)
        }
        
        // Return existing entries if optimization not complete
        return scheduleEntryRepository.findByScheduleId(scheduleId)
            .map { it.toResponse() }
    }
    
    fun applyOptimizedSchedule(scheduleId: UUID) {
        val optimizedEntries = getOptimizationResult(scheduleId)
        
        // Delete existing entries
        val existingEntries = scheduleEntryRepository.findByScheduleId(scheduleId)
        scheduleEntryRepository.deleteAll(existingEntries)
        
        // Create new optimized entries
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { IllegalArgumentException("Schedule not found with id: $scheduleId") }
        
        val newEntries = optimizedEntries.map { response ->
            ScheduleEntry(
                id = response.id,
                schedule = schedule,
                classId = response.classId,
                subjectId = response.subjectId,
                teacherId = response.teacherId,
                classroomId = response.classroomId,
                dayOfWeek = DayOfWeek.valueOf(response.dayOfWeek),
                startTime = LocalTime.parse(response.startTime),
                endTime = LocalTime.parse(response.endTime)
            )
        }
        
        scheduleEntryRepository.saveAll(newEntries)
    }
    
    private fun generateTimeSlots(): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()
        val days = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
        
        for (day in days) {
            var currentTime = LocalTime.of(8, 0)
            val endOfDay = LocalTime.of(16, 0)
            
            while (currentTime.plusHours(1) <= endOfDay) {
                slots.add(
                    TimeSlot(
                        dayOfWeek = day,
                        startTime = currentTime,
                        endTime = currentTime.plusHours(1)
                    )
                )
                currentTime = currentTime.plusHours(1)
            }
        }
        
        return slots
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
