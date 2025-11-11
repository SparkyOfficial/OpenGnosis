package com.opengnosis.scheduler.controller

import com.opengnosis.scheduler.dto.*
import com.opengnosis.scheduler.entity.ScheduleStatus
import com.opengnosis.scheduler.service.ScheduleConflictException
import com.opengnosis.scheduler.service.ScheduleService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/schedules")
class ScheduleController(
    private val scheduleService: ScheduleService,
    private val optimizationService: com.opengnosis.scheduler.service.OptimizationService
) {
    
    @PostMapping
    fun createSchedule(@RequestBody request: CreateScheduleRequest): ResponseEntity<ScheduleResponse> {
        val response = scheduleService.createSchedule(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
    
    @GetMapping("/{id}")
    fun getSchedule(@PathVariable id: UUID): ResponseEntity<ScheduleResponse> {
        val response = scheduleService.getSchedule(id)
        return ResponseEntity.ok(response)
    }
    
    @PostMapping("/{scheduleId}/entries")
    fun addScheduleEntry(
        @PathVariable scheduleId: UUID,
        @RequestBody request: CreateScheduleEntryRequest
    ): ResponseEntity<Any> {
        return try {
            val response = scheduleService.addScheduleEntry(scheduleId, request)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: ScheduleConflictException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf(
                    "message" to e.message,
                    "conflicts" to e.conflicts
                )
            )
        }
    }
    
    @PutMapping("/{scheduleId}/entries/{entryId}")
    fun updateScheduleEntry(
        @PathVariable scheduleId: UUID,
        @PathVariable entryId: UUID,
        @RequestBody request: CreateScheduleEntryRequest
    ): ResponseEntity<Any> {
        return try {
            val response = scheduleService.updateScheduleEntry(scheduleId, entryId, request)
            ResponseEntity.ok(response)
        } catch (e: ScheduleConflictException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                mapOf(
                    "message" to e.message,
                    "conflicts" to e.conflicts
                )
            )
        }
    }
    
    @DeleteMapping("/{scheduleId}/entries/{entryId}")
    fun deleteScheduleEntry(
        @PathVariable scheduleId: UUID,
        @PathVariable entryId: UUID
    ): ResponseEntity<Void> {
        scheduleService.deleteScheduleEntry(scheduleId, entryId)
        return ResponseEntity.noContent().build()
    }
    
    @PutMapping("/{scheduleId}/status")
    fun updateScheduleStatus(
        @PathVariable scheduleId: UUID,
        @RequestParam status: ScheduleStatus
    ): ResponseEntity<ScheduleResponse> {
        val response = scheduleService.updateScheduleStatus(scheduleId, status)
        return ResponseEntity.ok(response)
    }
    
    @PostMapping("/{scheduleId}/validate-entry")
    fun validateScheduleEntry(
        @PathVariable scheduleId: UUID,
        @RequestBody request: CreateScheduleEntryRequest
    ): ResponseEntity<ValidationResult> {
        val validation = scheduleService.validateScheduleEntry(
            scheduleId = scheduleId,
            classId = request.classId,
            teacherId = request.teacherId,
            classroomId = request.classroomId,
            dayOfWeek = java.time.DayOfWeek.valueOf(request.dayOfWeek.uppercase()),
            startTime = java.time.LocalTime.parse(request.startTime),
            endTime = java.time.LocalTime.parse(request.endTime)
        )
        return ResponseEntity.ok(validation)
    }
    
    @PostMapping("/{scheduleId}/optimize")
    fun optimizeSchedule(@PathVariable scheduleId: UUID): ResponseEntity<Map<String, Any>> {
        val jobId = optimizationService.optimizeSchedule(scheduleId)
        return ResponseEntity.accepted().body(
            mapOf(
                "message" to "Optimization started",
                "scheduleId" to jobId
            )
        )
    }
    
    @GetMapping("/{scheduleId}/optimization-result")
    fun getOptimizationResult(@PathVariable scheduleId: UUID): ResponseEntity<List<ScheduleEntryResponse>> {
        val result = optimizationService.getOptimizationResult(scheduleId)
        return ResponseEntity.ok(result)
    }
    
    @PostMapping("/{scheduleId}/apply-optimization")
    fun applyOptimizedSchedule(@PathVariable scheduleId: UUID): ResponseEntity<Map<String, String>> {
        optimizationService.applyOptimizedSchedule(scheduleId)
        return ResponseEntity.ok(mapOf("message" to "Optimized schedule applied successfully"))
    }
}
