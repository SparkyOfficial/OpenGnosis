package com.opengnosis.structure.service

import com.opengnosis.common.kafka.EventPublisher
import com.opengnosis.events.ClassCreatedEvent
import com.opengnosis.structure.domain.entity.ClassEntity
import com.opengnosis.structure.domain.entity.ClassStatus
import com.opengnosis.structure.dto.ClassResponse
import com.opengnosis.structure.dto.CreateClassRequest
import com.opengnosis.structure.dto.UpdateClassRequest
import com.opengnosis.structure.repository.ClassRepository
import com.opengnosis.structure.repository.EnrollmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ClassService(
    private val classRepository: ClassRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val eventPublisher: EventPublisher
) {
    
    @Transactional
    fun createClass(request: CreateClassRequest): ClassResponse {
        // Validate unique class name per school and academic year
        val existingClass = classRepository.findBySchoolIdAndAcademicYearIdAndName(
            request.schoolId,
            request.academicYearId,
            request.name
        )
        
        if (existingClass != null) {
            throw IllegalArgumentException(
                "Class with name '${request.name}' already exists for this school and academic year"
            )
        }
        
        val classEntity = ClassEntity(
            schoolId = request.schoolId,
            academicYearId = request.academicYearId,
            name = request.name,
            grade = request.grade,
            classTeacherId = request.classTeacherId,
            capacity = request.capacity,
            status = ClassStatus.ACTIVE
        )
        
        val savedClass = classRepository.save(classEntity)
        
        // Publish ClassCreatedEvent
        val event = ClassCreatedEvent(
            aggregateId = savedClass.id,
            schoolId = savedClass.schoolId,
            academicYearId = savedClass.academicYearId,
            name = savedClass.name,
            grade = savedClass.grade,
            classTeacherId = savedClass.classTeacherId
        )
        eventPublisher.publish(event)
        
        return toResponse(savedClass)
    }
    
    @Transactional(readOnly = true)
    fun getClass(id: UUID): ClassResponse {
        val classEntity = classRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Class not found with id: $id") }
        return toResponse(classEntity)
    }
    
    @Transactional(readOnly = true)
    fun getClassesBySchool(schoolId: UUID): List<ClassResponse> {
        return classRepository.findBySchoolId(schoolId).map { toResponse(it) }
    }
    
    @Transactional(readOnly = true)
    fun getClassesByAcademicYear(academicYearId: UUID): List<ClassResponse> {
        return classRepository.findByAcademicYearId(academicYearId).map { toResponse(it) }
    }
    
    @Transactional(readOnly = true)
    fun getClassesByTeacher(teacherId: UUID): List<ClassResponse> {
        return classRepository.findByClassTeacherId(teacherId).map { toResponse(it) }
    }
    
    @Transactional
    fun updateClass(id: UUID, request: UpdateClassRequest): ClassResponse {
        val classEntity = classRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Class not found with id: $id") }
        
        // Validate unique name if name is being changed
        if (request.name != null && request.name != classEntity.name) {
            val existingClass = classRepository.findBySchoolIdAndAcademicYearIdAndName(
                classEntity.schoolId,
                classEntity.academicYearId,
                request.name
            )
            if (existingClass != null) {
                throw IllegalArgumentException(
                    "Class with name '${request.name}' already exists for this school and academic year"
                )
            }
            classEntity.name = request.name
        }
        
        request.grade?.let { classEntity.grade = it }
        request.classTeacherId?.let { classEntity.classTeacherId = it }
        request.capacity?.let { classEntity.capacity = it }
        request.status?.let { classEntity.status = it }
        
        val updatedClass = classRepository.save(classEntity)
        return toResponse(updatedClass)
    }
    
    @Transactional
    fun deleteClass(id: UUID) {
        val classEntity = classRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Class not found with id: $id") }
        
        // Unenroll all active students from this class
        val activeEnrollments = enrollmentRepository.findByClassIdAndStatus(id, com.opengnosis.domain.EnrollmentStatus.ACTIVE)
        
        activeEnrollments.forEach { enrollment ->
            enrollment.status = com.opengnosis.domain.EnrollmentStatus.WITHDRAWN
            enrollment.unenrollmentDate = java.time.LocalDate.now()
            enrollmentRepository.save(enrollment)
            
            // Publish StudentUnenrolledEvent for each student
            val event = com.opengnosis.events.StudentUnenrolledEvent(
                aggregateId = enrollment.id,
                studentId = enrollment.studentId,
                classId = enrollment.classId,
                reason = "Class deleted"
            )
            eventPublisher.publish(event)
        }
        
        // Soft delete the class
        classEntity.status = ClassStatus.DELETED
        classRepository.save(classEntity)
    }
    
    private fun toResponse(classEntity: ClassEntity): ClassResponse {
        return ClassResponse(
            id = classEntity.id,
            schoolId = classEntity.schoolId,
            academicYearId = classEntity.academicYearId,
            name = classEntity.name,
            grade = classEntity.grade,
            classTeacherId = classEntity.classTeacherId,
            capacity = classEntity.capacity,
            status = classEntity.status,
            createdAt = classEntity.createdAt,
            updatedAt = classEntity.updatedAt
        )
    }
}
