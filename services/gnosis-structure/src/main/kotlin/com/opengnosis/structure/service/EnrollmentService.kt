package com.opengnosis.structure.service

import com.opengnosis.common.kafka.EventPublisher
import com.opengnosis.domain.EnrollmentStatus
import com.opengnosis.events.StudentEnrolledEvent
import com.opengnosis.events.StudentUnenrolledEvent
import com.opengnosis.structure.domain.entity.EnrollmentEntity
import com.opengnosis.structure.dto.*
import com.opengnosis.structure.repository.ClassRepository
import com.opengnosis.structure.repository.EnrollmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class EnrollmentService(
    private val enrollmentRepository: EnrollmentRepository,
    private val classRepository: ClassRepository,
    private val eventPublisher: EventPublisher
) {
    
    @Transactional
    fun enrollStudent(request: EnrollStudentRequest): EnrollmentResponse {
        // Check if student is already enrolled in this class
        val existingEnrollment = enrollmentRepository.findByStudentIdAndClassIdAndStatus(
            request.studentId,
            request.classId,
            EnrollmentStatus.ACTIVE
        )
        
        if (existingEnrollment != null) {
            throw IllegalArgumentException(
                "Student is already enrolled in this class"
            )
        }
        
        // Verify class exists and has capacity
        val classEntity = classRepository.findById(request.classId)
            .orElseThrow { IllegalArgumentException("Class not found with id: ${request.classId}") }
        
        val currentEnrollmentCount = enrollmentRepository
            .findByClassIdAndStatus(request.classId, EnrollmentStatus.ACTIVE)
            .size
        
        if (currentEnrollmentCount >= classEntity.capacity) {
            throw IllegalStateException("Class has reached maximum capacity")
        }
        
        val enrollment = EnrollmentEntity(
            studentId = request.studentId,
            classId = request.classId,
            enrollmentDate = request.enrollmentDate,
            status = EnrollmentStatus.ACTIVE
        )
        
        val savedEnrollment = enrollmentRepository.save(enrollment)
        
        // Publish StudentEnrolledEvent
        val event = StudentEnrolledEvent(
            aggregateId = savedEnrollment.id,
            studentId = savedEnrollment.studentId,
            classId = savedEnrollment.classId,
            enrollmentDate = savedEnrollment.enrollmentDate
        )
        eventPublisher.publish(event)
        
        return toResponse(savedEnrollment)
    }
    
    @Transactional
    fun unenrollStudent(enrollmentId: UUID, request: UnenrollStudentRequest): EnrollmentResponse {
        val enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow { IllegalArgumentException("Enrollment not found with id: $enrollmentId") }
        
        if (enrollment.status != EnrollmentStatus.ACTIVE) {
            throw IllegalStateException("Enrollment is not active")
        }
        
        enrollment.status = EnrollmentStatus.WITHDRAWN
        enrollment.unenrollmentDate = request.unenrollmentDate
        
        val updatedEnrollment = enrollmentRepository.save(enrollment)
        
        // Publish StudentUnenrolledEvent
        val event = StudentUnenrolledEvent(
            aggregateId = updatedEnrollment.id,
            studentId = updatedEnrollment.studentId,
            classId = updatedEnrollment.classId,
            reason = request.reason
        )
        eventPublisher.publish(event)
        
        return toResponse(updatedEnrollment)
    }
    
    @Transactional(readOnly = true)
    fun getEnrollment(enrollmentId: UUID): EnrollmentResponse {
        val enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow { IllegalArgumentException("Enrollment not found with id: $enrollmentId") }
        return toResponse(enrollment)
    }
    
    @Transactional(readOnly = true)
    fun getStudentEnrollments(studentId: UUID): List<EnrollmentResponse> {
        return enrollmentRepository.findByStudentId(studentId).map { toResponse(it) }
    }
    
    @Transactional(readOnly = true)
    fun getClassComposition(classId: UUID): ClassCompositionResponse {
        val classEntity = classRepository.findById(classId)
            .orElseThrow { IllegalArgumentException("Class not found with id: $classId") }
        
        val enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE)
        
        val students = enrollments.map { enrollment ->
            StudentEnrollmentInfo(
                enrollmentId = enrollment.id,
                studentId = enrollment.studentId,
                enrollmentDate = enrollment.enrollmentDate,
                status = enrollment.status
            )
        }
        
        return ClassCompositionResponse(
            classId = classEntity.id,
            className = classEntity.name,
            totalStudents = students.size,
            capacity = classEntity.capacity,
            students = students
        )
    }
    
    private fun toResponse(enrollment: EnrollmentEntity): EnrollmentResponse {
        return EnrollmentResponse(
            id = enrollment.id,
            studentId = enrollment.studentId,
            classId = enrollment.classId,
            enrollmentDate = enrollment.enrollmentDate,
            unenrollmentDate = enrollment.unenrollmentDate,
            status = enrollment.status,
            createdAt = enrollment.createdAt,
            updatedAt = enrollment.updatedAt
        )
    }
}
