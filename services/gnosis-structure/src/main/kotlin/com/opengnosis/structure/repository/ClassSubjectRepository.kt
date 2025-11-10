package com.opengnosis.structure.repository

import com.opengnosis.structure.domain.entity.ClassSubjectEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ClassSubjectRepository : JpaRepository<ClassSubjectEntity, UUID> {
    fun findByClassId(classId: UUID): List<ClassSubjectEntity>
    fun findBySubjectId(subjectId: UUID): List<ClassSubjectEntity>
    fun findByTeacherId(teacherId: UUID): List<ClassSubjectEntity>
    fun findByClassIdAndSubjectId(classId: UUID, subjectId: UUID): ClassSubjectEntity?
}
