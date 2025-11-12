package com.opengnosis.scheduler.service

import com.opengnosis.scheduler.dto.ClassroomResponse
import com.opengnosis.scheduler.dto.CreateClassroomRequest
import com.opengnosis.scheduler.dto.UpdateClassroomRequest
import com.opengnosis.scheduler.entity.Classroom
import com.opengnosis.scheduler.repository.ClassroomRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ClassroomService(
    private val classroomRepository: ClassroomRepository
) {
    
    @Transactional
    fun createClassroom(request: CreateClassroomRequest): ClassroomResponse {
        val classroom = Classroom(
            schoolId = request.schoolId,
            name = request.name,
            capacity = request.capacity
        )
        
        val saved = classroomRepository.save(classroom)
        return saved.toResponse()
    }
    
    @Transactional(readOnly = true)
    fun getClassroom(id: UUID): ClassroomResponse {
        val classroom = classroomRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Classroom not found with id: $id") }
        return classroom.toResponse()
    }
    
    @Transactional(readOnly = true)
    fun getClassroomsBySchool(schoolId: UUID): List<ClassroomResponse> {
        return classroomRepository.findBySchoolId(schoolId)
            .map { it.toResponse() }
    }
    
    @Transactional
    fun updateClassroom(id: UUID, request: UpdateClassroomRequest): ClassroomResponse {
        val classroom = classroomRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Classroom not found with id: $id") }
        
        val updated = classroom.copy(
            name = request.name ?: classroom.name,
            capacity = request.capacity ?: classroom.capacity
        )
        
        val saved = classroomRepository.save(updated)
        return saved.toResponse()
    }
    
    @Transactional
    fun deleteClassroom(id: UUID) {
        if (!classroomRepository.existsById(id)) {
            throw IllegalArgumentException("Classroom not found with id: $id")
        }
        classroomRepository.deleteById(id)
    }
    
    private fun Classroom.toResponse() = ClassroomResponse(
        id = id,
        schoolId = schoolId,
        name = name,
        capacity = capacity
    )
}
