package com.opengnosis.structure.controller

import com.opengnosis.structure.dto.*
import com.opengnosis.structure.service.EnrollmentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class EnrollmentController(
    private val enrollmentService: EnrollmentService
) {
    
    @PostMapping("/enrollments")
    fun enrollStudent(@Valid @RequestBody request: EnrollStudentRequest): ResponseEntity<EnrollmentResponse> {
        val enrollment = enrollmentService.enrollStudent(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment)
    }
    
    @PostMapping("/enrollments/{enrollmentId}/unenroll")
    fun unenrollStudent(
        @PathVariable enrollmentId: UUID,
        @Valid @RequestBody request: UnenrollStudentRequest
    ): ResponseEntity<EnrollmentResponse> {
        val enrollment = enrollmentService.unenrollStudent(enrollmentId, request)
        return ResponseEntity.ok(enrollment)
    }
    
    @GetMapping("/enrollments/{enrollmentId}")
    fun getEnrollment(@PathVariable enrollmentId: UUID): ResponseEntity<EnrollmentResponse> {
        val enrollment = enrollmentService.getEnrollment(enrollmentId)
        return ResponseEntity.ok(enrollment)
    }
    
    @GetMapping("/students/{studentId}/enrollments")
    fun getStudentEnrollments(@PathVariable studentId: UUID): ResponseEntity<List<EnrollmentResponse>> {
        val enrollments = enrollmentService.getStudentEnrollments(studentId)
        return ResponseEntity.ok(enrollments)
    }
    
    @GetMapping("/classes/{classId}/composition")
    fun getClassComposition(@PathVariable classId: UUID): ResponseEntity<ClassCompositionResponse> {
        val composition = enrollmentService.getClassComposition(classId)
        return ResponseEntity.ok(composition)
    }
}
