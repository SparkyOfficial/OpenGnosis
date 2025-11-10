package com.opengnosis.structure.controller

import com.opengnosis.structure.dto.*
import com.opengnosis.structure.service.SubjectService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/subjects")
class SubjectController(
    private val subjectService: SubjectService
) {
    
    @PostMapping
    fun createSubject(@Valid @RequestBody request: CreateSubjectRequest): ResponseEntity<SubjectResponse> {
        val subject = subjectService.createSubject(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(subject)
    }
    
    @GetMapping("/{subjectId}")
    fun getSubject(@PathVariable subjectId: UUID): ResponseEntity<SubjectResponse> {
        val subject = subjectService.getSubject(subjectId)
        return ResponseEntity.ok(subject)
    }
    
    @GetMapping
    fun getSubjects(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) schoolId: UUID?,
        @RequestParam(required = false) grade: Int?
    ): ResponseEntity<List<SubjectResponse>> {
        val subjects = when {
            search != null -> subjectService.searchSubjects(search)
            schoolId != null && grade != null -> subjectService.getSubjectsBySchoolAndGrade(schoolId, grade)
            else -> subjectService.getAllSubjects()
        }
        return ResponseEntity.ok(subjects)
    }
    
    @PutMapping("/{subjectId}")
    fun updateSubject(
        @PathVariable subjectId: UUID,
        @Valid @RequestBody request: UpdateSubjectRequest
    ): ResponseEntity<SubjectResponse> {
        val subject = subjectService.updateSubject(subjectId, request)
        return ResponseEntity.ok(subject)
    }
    
    @DeleteMapping("/{subjectId}")
    fun deleteSubject(@PathVariable subjectId: UUID): ResponseEntity<Void> {
        subjectService.deleteSubject(subjectId)
        return ResponseEntity.noContent().build()
    }
    
    @PostMapping("/assign")
    fun assignSubjectToClass(@Valid @RequestBody request: AssignSubjectToClassRequest): ResponseEntity<ClassSubjectResponse> {
        val classSubject = subjectService.assignSubjectToClass(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(classSubject)
    }
    
    @GetMapping("/class/{classId}")
    fun getClassSubjects(@PathVariable classId: UUID): ResponseEntity<List<ClassSubjectResponse>> {
        val subjects = subjectService.getClassSubjects(classId)
        return ResponseEntity.ok(subjects)
    }
    
    @DeleteMapping("/class/{classId}/subject/{subjectId}")
    fun removeSubjectFromClass(
        @PathVariable classId: UUID,
        @PathVariable subjectId: UUID
    ): ResponseEntity<Void> {
        subjectService.removeSubjectFromClass(classId, subjectId)
        return ResponseEntity.noContent().build()
    }
}
