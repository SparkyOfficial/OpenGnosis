package com.opengnosis.structure.controller

import com.opengnosis.structure.dto.ClassResponse
import com.opengnosis.structure.dto.CreateClassRequest
import com.opengnosis.structure.dto.UpdateClassRequest
import com.opengnosis.structure.service.ClassService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/classes")
class ClassController(
    private val classService: ClassService
) {
    
    @PostMapping
    fun createClass(@Valid @RequestBody request: CreateClassRequest): ResponseEntity<ClassResponse> {
        val classResponse = classService.createClass(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(classResponse)
    }
    
    @GetMapping("/{classId}")
    fun getClass(@PathVariable classId: UUID): ResponseEntity<ClassResponse> {
        val classResponse = classService.getClass(classId)
        return ResponseEntity.ok(classResponse)
    }
    
    @GetMapping
    fun getClasses(
        @RequestParam(required = false) schoolId: UUID?,
        @RequestParam(required = false) academicYearId: UUID?,
        @RequestParam(required = false) teacherId: UUID?
    ): ResponseEntity<List<ClassResponse>> {
        val classes = when {
            schoolId != null -> classService.getClassesBySchool(schoolId)
            academicYearId != null -> classService.getClassesByAcademicYear(academicYearId)
            teacherId != null -> classService.getClassesByTeacher(teacherId)
            else -> emptyList()
        }
        return ResponseEntity.ok(classes)
    }
    
    @PutMapping("/{classId}")
    fun updateClass(
        @PathVariable classId: UUID,
        @Valid @RequestBody request: UpdateClassRequest
    ): ResponseEntity<ClassResponse> {
        val classResponse = classService.updateClass(classId, request)
        return ResponseEntity.ok(classResponse)
    }
    
    @DeleteMapping("/{classId}")
    fun deleteClass(@PathVariable classId: UUID): ResponseEntity<Void> {
        classService.deleteClass(classId)
        return ResponseEntity.noContent().build()
    }
}
