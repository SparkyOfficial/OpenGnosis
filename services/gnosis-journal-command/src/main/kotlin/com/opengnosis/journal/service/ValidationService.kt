package com.opengnosis.journal.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.util.UUID

@Service
class ValidationService(
    private val webClientBuilder: WebClient.Builder
) {
    
    private val structureServiceClient = webClientBuilder
        .baseUrl("http://gnosis-structure:8082")
        .build()
    
    suspend fun validateStudentSubjectAssociation(studentId: UUID, subjectId: UUID): Boolean {
        return try {
            // Check if student is enrolled in a class that has this subject
            val response = structureServiceClient
                .get()
                .uri("/api/v1/students/{studentId}/subjects/{subjectId}/validate", studentId, subjectId)
                .retrieve()
                .awaitBody<ValidationResponse>()
            response.valid
        } catch (e: Exception) {
            // If validation service is unavailable, we'll accept the command
            // and let eventual consistency handle it
            true
        }
    }
    
    suspend fun validateStudentEnrollment(studentId: UUID, classId: UUID): Boolean {
        return try {
            val response = structureServiceClient
                .get()
                .uri("/api/v1/students/{studentId}/classes/{classId}/validate", studentId, classId)
                .retrieve()
                .awaitBody<ValidationResponse>()
            response.valid
        } catch (e: Exception) {
            true
        }
    }
    
    suspend fun validateTeacherClassSubjectAssociation(
        teacherId: UUID,
        classId: UUID,
        subjectId: UUID
    ): Boolean {
        return try {
            val response = structureServiceClient
                .get()
                .uri("/api/v1/teachers/{teacherId}/classes/{classId}/subjects/{subjectId}/validate",
                    teacherId, classId, subjectId)
                .retrieve()
                .awaitBody<ValidationResponse>()
            response.valid
        } catch (e: Exception) {
            true
        }
    }
    
    fun validateGradeValue(gradeValue: Int): Boolean {
        return gradeValue in 1..10
    }
}

data class ValidationResponse(val valid: Boolean)
