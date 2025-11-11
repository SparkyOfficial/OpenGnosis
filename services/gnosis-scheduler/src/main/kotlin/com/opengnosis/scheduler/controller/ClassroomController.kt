package com.opengnosis.scheduler.controller

import com.opengnosis.scheduler.dto.ClassroomResponse
import com.opengnosis.scheduler.dto.CreateClassroomRequest
import com.opengnosis.scheduler.dto.UpdateClassroomRequest
import com.opengnosis.scheduler.service.ClassroomService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/classrooms")
class ClassroomController(
    private val classroomService: ClassroomService
) {
    
    @PostMapping
    fun createClassroom(@RequestBody request: CreateClassroomRequest): ResponseEntity<ClassroomResponse> {
        val response = classroomService.createClassroom(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
    
    @GetMapping("/{id}")
    fun getClassroom(@PathVariable id: UUID): ResponseEntity<ClassroomResponse> {
        val response = classroomService.getClassroom(id)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/school/{schoolId}")
    fun getClassroomsBySchool(@PathVariable schoolId: UUID): ResponseEntity<List<ClassroomResponse>> {
        val response = classroomService.getClassroomsBySchool(schoolId)
        return ResponseEntity.ok(response)
    }
    
    @PutMapping("/{id}")
    fun updateClassroom(
        @PathVariable id: UUID,
        @RequestBody request: UpdateClassroomRequest
    ): ResponseEntity<ClassroomResponse> {
        val response = classroomService.updateClassroom(id, request)
        return ResponseEntity.ok(response)
    }
    
    @DeleteMapping("/{id}")
    fun deleteClassroom(@PathVariable id: UUID): ResponseEntity<Void> {
        classroomService.deleteClassroom(id)
        return ResponseEntity.noContent().build()
    }
}
