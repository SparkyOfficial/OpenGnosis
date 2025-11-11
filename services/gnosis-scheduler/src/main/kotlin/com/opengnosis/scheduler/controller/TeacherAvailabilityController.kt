package com.opengnosis.scheduler.controller

import com.opengnosis.scheduler.dto.TeacherAvailabilityRequest
import com.opengnosis.scheduler.dto.TeacherAvailabilityResponse
import com.opengnosis.scheduler.service.TeacherAvailabilityService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/teacher-availability")
class TeacherAvailabilityController(
    private val teacherAvailabilityService: TeacherAvailabilityService
) {
    
    @PostMapping
    fun createAvailability(@RequestBody request: TeacherAvailabilityRequest): ResponseEntity<TeacherAvailabilityResponse> {
        val response = teacherAvailabilityService.createAvailability(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
    
    @GetMapping("/teacher/{teacherId}")
    fun getTeacherAvailability(@PathVariable teacherId: UUID): ResponseEntity<List<TeacherAvailabilityResponse>> {
        val response = teacherAvailabilityService.getTeacherAvailability(teacherId)
        return ResponseEntity.ok(response)
    }
    
    @DeleteMapping("/{id}")
    fun deleteAvailability(@PathVariable id: UUID): ResponseEntity<Void> {
        teacherAvailabilityService.deleteAvailability(id)
        return ResponseEntity.noContent().build()
    }
}
