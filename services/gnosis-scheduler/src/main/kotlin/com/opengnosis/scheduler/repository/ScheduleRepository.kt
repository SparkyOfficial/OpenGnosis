package com.opengnosis.scheduler.repository

import com.opengnosis.scheduler.entity.Schedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduleRepository : JpaRepository<Schedule, UUID> {
    fun findByAcademicYearId(academicYearId: UUID): List<Schedule>
    fun findByTermId(termId: UUID): List<Schedule>
}
