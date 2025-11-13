package com.opengnosis.analytics.controller

import com.opengnosis.analytics.document.StudentSearchDocument
import com.opengnosis.analytics.service.SearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/analytics/search")
class SearchController(
    private val searchService: SearchService
) {
    
    @GetMapping("/students")
    fun searchStudents(
        @RequestParam(required = false) q: String?
    ): ResponseEntity<List<StudentSearchDocument>> {
        if (q.isNullOrBlank()) {
            return ResponseEntity.ok(emptyList())
        }
        
        val results = searchService.searchStudents(q)
        return ResponseEntity.ok(results)
    }
    
    @GetMapping("/students/advanced")
    fun advancedStudentSearch(
        @RequestParam(required = false) firstName: String?,
        @RequestParam(required = false) lastName: String?,
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) classId: String?
    ): ResponseEntity<List<StudentSearchDocument>> {
        val results = searchService.advancedStudentSearch(
            firstName = firstName,
            lastName = lastName,
            email = email,
            classId = classId
        )
        return ResponseEntity.ok(results)
    }
    
    @GetMapping("/students/by-email")
    fun searchStudentByEmail(
        @RequestParam email: String
    ): ResponseEntity<StudentSearchDocument?> {
        val result = searchService.searchStudentsByEmail(email)
        return if (result != null) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/students/by-id")
    fun searchStudentById(
        @RequestParam studentId: String
    ): ResponseEntity<StudentSearchDocument?> {
        val result = searchService.searchStudentById(studentId)
        return if (result != null) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
