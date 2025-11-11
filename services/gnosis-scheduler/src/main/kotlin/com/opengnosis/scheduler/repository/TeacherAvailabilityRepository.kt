package com.opengnosis.scheduler.repository

import com.opengnosis.scheduler.entity.TeacherAvailability
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.DayOfWeek
import java.util.UUID

@Repository
interface TeacherAvailabilityRepository : JpaRepository<TeacherAvailability, UUID> {
    fun findByTeacherId(teacherId: UUID): List<TeacherAvailability>
    fun findByTeacherIdAndDayOfWeek(teacherId: UUID, dayOfWeek: DayOfWeek): List<TeacherAvailability>
}
