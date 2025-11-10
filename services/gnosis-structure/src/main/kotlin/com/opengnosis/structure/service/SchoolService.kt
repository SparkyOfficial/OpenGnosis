package com.opengnosis.structure.service

import com.opengnosis.common.kafka.EventPublisher
import com.opengnosis.domain.SchoolStatus
import com.opengnosis.events.SchoolCreatedEvent
import com.opengnosis.structure.domain.entity.SchoolEntity
import com.opengnosis.structure.dto.CreateSchoolRequest
import com.opengnosis.structure.dto.SchoolResponse
import com.opengnosis.structure.dto.UpdateSchoolRequest
import com.opengnosis.structure.repository.ClassRepository
import com.opengnosis.structure.repository.SchoolRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SchoolService(
    private val schoolRepository: SchoolRepository,
    private val classRepository: ClassRepository,
    private val eventPublisher: EventPublisher
) {
    
    @Transactional
    fun createSchool(request: CreateSchoolRequest): SchoolResponse {
        val school = SchoolEntity(
            name = request.name,
            address = request.address,
            principalId = request.principalId,
            status = SchoolStatus.ACTIVE
        )
        
        val savedSchool = schoolRepository.save(school)
        
        // Publish SchoolCreatedEvent
        val event = SchoolCreatedEvent(
            aggregateId = savedSchool.id,
            name = savedSchool.name,
            address = savedSchool.address,
            principalId = savedSchool.principalId
        )
        eventPublisher.publish(event)
        
        return toResponse(savedSchool)
    }
    
    @Transactional(readOnly = true)
    fun getSchool(id: UUID): SchoolResponse {
        val school = schoolRepository.findById(id)
            .orElseThrow { IllegalArgumentException("School not found with id: $id") }
        return toResponse(school)
    }
    
    @Transactional(readOnly = true)
    fun getAllSchools(): List<SchoolResponse> {
        return schoolRepository.findAll().map { toResponse(it) }
    }
    
    @Transactional(readOnly = true)
    fun getActiveSchools(): List<SchoolResponse> {
        return schoolRepository.findByStatus(SchoolStatus.ACTIVE).map { toResponse(it) }
    }
    
    @Transactional
    fun updateSchool(id: UUID, request: UpdateSchoolRequest): SchoolResponse {
        val school = schoolRepository.findById(id)
            .orElseThrow { IllegalArgumentException("School not found with id: $id") }
        
        request.name?.let { school.name = it }
        request.address?.let { school.address = it }
        request.principalId?.let { school.principalId = it }
        request.status?.let { school.status = it }
        
        val updatedSchool = schoolRepository.save(school)
        return toResponse(updatedSchool)
    }
    
    @Transactional
    fun deleteSchool(id: UUID) {
        val school = schoolRepository.findById(id)
            .orElseThrow { IllegalArgumentException("School not found with id: $id") }
        
        // Check if school has active classes
        val activeClasses = classRepository.findBySchoolIdAndStatus(id, com.opengnosis.structure.domain.entity.ClassStatus.ACTIVE)
        if (activeClasses.isNotEmpty()) {
            throw IllegalStateException("Cannot delete school with active classes. Please delete or archive all classes first.")
        }
        
        // Soft delete
        school.status = SchoolStatus.ARCHIVED
        schoolRepository.save(school)
    }
    
    private fun toResponse(school: SchoolEntity): SchoolResponse {
        return SchoolResponse(
            id = school.id,
            name = school.name,
            address = school.address,
            principalId = school.principalId,
            status = school.status,
            createdAt = school.createdAt,
            updatedAt = school.updatedAt
        )
    }
}
