package com.opengnosis.structure.controller

import com.opengnosis.structure.dto.CreateSchoolRequest
import com.opengnosis.structure.dto.SchoolResponse
import com.opengnosis.structure.dto.UpdateSchoolRequest
import com.opengnosis.structure.service.SchoolService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/schools")
class SchoolController(
    private val schoolService: SchoolService
) {
    
    @PostMapping
    fun createSchool(@Valid @RequestBody request: CreateSchoolRequest): ResponseEntity<SchoolResponse> {
        val school = schoolService.createSchool(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(school)
    }
    
    @GetMapping("/{schoolId}")
    fun getSchool(@PathVariable schoolId: UUID): ResponseEntity<SchoolResponse> {
        val school = schoolService.getSchool(schoolId)
        return ResponseEntity.ok(school)
    }
    
    @GetMapping
    fun getAllSchools(@RequestParam(required = false) activeOnly: Boolean = false): ResponseEntity<List<SchoolResponse>> {
        val schools = if (activeOnly) {
            schoolService.getActiveSchools()
        } else {
            schoolService.getAllSchools()
        }
        return ResponseEntity.ok(schools)
    }
    
    @PutMapping("/{schoolId}")
    fun updateSchool(
        @PathVariable schoolId: UUID,
        @Valid @RequestBody request: UpdateSchoolRequest
    ): ResponseEntity<SchoolResponse> {
        val school = schoolService.updateSchool(schoolId, request)
        return ResponseEntity.ok(school)
    }
    
    @DeleteMapping("/{schoolId}")
    fun deleteSchool(@PathVariable schoolId: UUID): ResponseEntity<Void> {
        schoolService.deleteSchool(schoolId)
        return ResponseEntity.noContent().build()
    }
}
