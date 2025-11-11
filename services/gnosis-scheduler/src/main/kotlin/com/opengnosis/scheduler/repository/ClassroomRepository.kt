package com.opengnosis.scheduler.repository

import com.opengnosis.scheduler.entity.Classroom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ClassroomRepository : JpaRepository<Classroom, UUID> {
    fun findBySchoolId(schoolId: UUID): List<Classroom>
}
