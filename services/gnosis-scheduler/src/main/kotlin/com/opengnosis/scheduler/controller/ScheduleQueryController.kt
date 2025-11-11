package com.opengnosis.scheduler.controller

import com.opengnosis.scheduler.dto.ScheduleEntryResponse
import com.opengnosis.scheduler.service.ScheduleQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/schedules")
class ScheduleQueryController(
    private val scheduleQueryService: ScheduleQueryService
) {
    
    @GetMapping("/class/{classId}")
    fun getScheduleByClass(@PathVariable classId: UUID): ResponseEntity<List<ScheduleEntryResponse>> {
        val response = scheduleQueryService.getScheduleByClass(classId)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/teacher/{teacherId}")
    fun getScheduleByTeacher(@PathVariable teacherId: UUID): ResponseEntity<List<ScheduleEntryResponse>> {
        val response = scheduleQueryService.getScheduleByTeacher(teacherId)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/classroom/{classroomId}")
    fun getScheduleByClassroom(@PathVariable classroomId: UUID): ResponseEntity<List<ScheduleEntryResponse>> {
        val response = scheduleQueryService.getScheduleByClassroom(classroomId)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/entry/{entryId}")
    fun getScheduleEntry(@PathVariable entryId: UUID): ResponseEntity<ScheduleEntryResponse> {
        val response = scheduleQueryService.getScheduleEntry(entryId)
        return ResponseEntity.ok(response)
    }
}
