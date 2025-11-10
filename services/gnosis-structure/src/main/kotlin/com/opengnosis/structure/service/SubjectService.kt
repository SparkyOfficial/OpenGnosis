package com.opengnosis.structure.service

import com.opengnosis.events.TeacherAssignedEvent
import com.opengnosis.common.kafka.EventPublisher
import com.opengnosis.structure.domain.entity.ClassSubjectEntity
import com.opengnosis.structure.domain.entity.SubjectEntity
import com.opengnosis.structure.dto.*
import com.opengnosis.structure.repository.ClassRepository
import com.opengnosis.structure.repository.ClassSubjectRepository
import com.opengnosis.structure.repository.SubjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SubjectService(
    private val subjectRepository: SubjectRepository,
    private val classSubjectRepository: ClassSubjectRepository,
    private val classRepository: ClassRepository,
    private val eventPublisher: EventPublisher
) {
    
    @Transactional
    fun createSubject(request: CreateSubjectRequest): SubjectResponse {
        // Check if subject code already exists
        val existingSubject = subjectRepository.findByCode(request.code)
        if (existingSubject != null) {
            throw IllegalArgumentException("Subject with code '${request.code}' already exists")
        }
        
        val subject = SubjectEntity(
            name = request.name,
            code = request.code,
            description = request.description
        )
        
        val savedSubject = subjectRepository.save(subject)
        return toResponse(savedSubject)
    }
    
    @Transactional(readOnly = true)
    fun getSubject(id: UUID): SubjectResponse {
        val subject = subjectRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Subject not found with id: $id") }
        return toResponse(subject)
    }
    
    @Transactional(readOnly = true)
    fun getAllSubjects(): List<SubjectResponse> {
        return subjectRepository.findAll().map { toResponse(it) }
    }
    
    @Transactional(readOnly = true)
    fun searchSubjects(query: String): List<SubjectResponse> {
        return subjectRepository.findByNameContainingIgnoreCase(query).map { toResponse(it) }
    }
    
    @Transactional
    fun updateSubject(id: UUID, request: UpdateSubjectRequest): SubjectResponse {
        val subject = subjectRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Subject not found with id: $id") }
        
        request.name?.let { subject.name = it }
        request.description?.let { subject.description = it }
        
        val updatedSubject = subjectRepository.save(subject)
        return toResponse(updatedSubject)
    }
    
    @Transactional
    fun deleteSubject(id: UUID) {
        if (!subjectRepository.existsById(id)) {
            throw IllegalArgumentException("Subject not found with id: $id")
        }
        subjectRepository.deleteById(id)
    }
    
    @Transactional
    fun assignSubjectToClass(request: AssignSubjectToClassRequest): ClassSubjectResponse {
        // Verify class exists
        val classEntity = classRepository.findById(request.classId)
            .orElseThrow { IllegalArgumentException("Class not found with id: ${request.classId}") }
        
        // Verify subject exists
        val subject = subjectRepository.findById(request.subjectId)
            .orElseThrow { IllegalArgumentException("Subject not found with id: ${request.subjectId}") }
        
        // Check if subject is already assigned to this class
        val existingAssignment = classSubjectRepository.findByClassIdAndSubjectId(
            request.classId,
            request.subjectId
        )
        
        if (existingAssignment != null) {
            throw IllegalArgumentException("Subject is already assigned to this class")
        }
        
        val classSubject = ClassSubjectEntity(
            classId = request.classId,
            subjectId = request.subjectId,
            teacherId = request.teacherId,
            hoursPerWeek = request.hoursPerWeek
        )
        
        val savedClassSubject = classSubjectRepository.save(classSubject)
        
        // Publish TeacherAssignedEvent
        val event = TeacherAssignedEvent(
            aggregateId = savedClassSubject.id,
            teacherId = savedClassSubject.teacherId,
            classId = savedClassSubject.classId,
            subjectId = savedClassSubject.subjectId
        )
        eventPublisher.publish(event)
        
        return toClassSubjectResponse(savedClassSubject, subject)
    }
    
    @Transactional(readOnly = true)
    fun getClassSubjects(classId: UUID): List<ClassSubjectResponse> {
        val classSubjects = classSubjectRepository.findByClassId(classId)
        return classSubjects.map { classSubject ->
            val subject = subjectRepository.findById(classSubject.subjectId)
                .orElseThrow { IllegalArgumentException("Subject not found") }
            toClassSubjectResponse(classSubject, subject)
        }
    }
    
    @Transactional(readOnly = true)
    fun getSubjectsBySchoolAndGrade(schoolId: UUID, grade: Int): List<SubjectResponse> {
        // Get all classes for the school and grade
        val classes = classRepository.findBySchoolId(schoolId)
            .filter { it.grade == grade }
        
        // Get all subjects assigned to these classes
        val subjectIds = classes.flatMap { classEntity ->
            classSubjectRepository.findByClassId(classEntity.id)
                .map { it.subjectId }
        }.distinct()
        
        return subjectIds.mapNotNull { subjectId ->
            subjectRepository.findById(subjectId).map { toResponse(it) }.orElse(null)
        }
    }
    
    @Transactional
    fun removeSubjectFromClass(classId: UUID, subjectId: UUID) {
        val classSubject = classSubjectRepository.findByClassIdAndSubjectId(classId, subjectId)
            ?: throw IllegalArgumentException("Subject is not assigned to this class")
        
        classSubjectRepository.delete(classSubject)
    }
    
    private fun toResponse(subject: SubjectEntity): SubjectResponse {
        return SubjectResponse(
            id = subject.id,
            name = subject.name,
            code = subject.code,
            description = subject.description,
            createdAt = subject.createdAt
        )
    }
    
    private fun toClassSubjectResponse(
        classSubject: ClassSubjectEntity,
        subject: SubjectEntity
    ): ClassSubjectResponse {
        return ClassSubjectResponse(
            id = classSubject.id,
            classId = classSubject.classId,
            subjectId = classSubject.subjectId,
            subjectName = subject.name,
            subjectCode = subject.code,
            teacherId = classSubject.teacherId,
            hoursPerWeek = classSubject.hoursPerWeek,
            createdAt = classSubject.createdAt
        )
    }
}
