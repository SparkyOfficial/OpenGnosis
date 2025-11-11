package com.opengnosis.scheduler.repository

import com.opengnosis.scheduler.entity.ScheduleEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Repository
interface ScheduleEntryRepository : JpaRepository<ScheduleEntry, UUID> {
    fun findByScheduleId(scheduleId: UUID): List<ScheduleEntry>
    fun findByClassId(classId: UUID): List<ScheduleEntry>
    fun findByTeacherId(teacherId: UUID): List<ScheduleEntry>
    fun findByClassroomId(classroomId: UUID): List<ScheduleEntry>
    
    @Query("""
        SELECT se FROM ScheduleEntry se 
        WHERE se.schedule.id = :scheduleId 
        AND se.teacherId = :teacherId 
        AND se.dayOfWeek = :dayOfWeek 
        AND ((se.startTime < :endTime AND se.endTime > :startTime))
    """)
    fun findTeacherConflicts(
        scheduleId: UUID,
        teacherId: UUID,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime
    ): List<ScheduleEntry>
    
    @Query("""
        SELECT se FROM ScheduleEntry se 
        WHERE se.schedule.id = :scheduleId 
        AND se.classroomId = :classroomId 
        AND se.dayOfWeek = :dayOfWeek 
        AND ((se.startTime < :endTime AND se.endTime > :startTime))
    """)
    fun findClassroomConflicts(
        scheduleId: UUID,
        classroomId: UUID,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime
    ): List<ScheduleEntry>
    
    @Query("""
        SELECT se FROM ScheduleEntry se 
        WHERE se.schedule.id = :scheduleId 
        AND se.classId = :classId 
        AND se.dayOfWeek = :dayOfWeek 
        AND ((se.startTime < :endTime AND se.endTime > :startTime))
    """)
    fun findClassConflicts(
        scheduleId: UUID,
        classId: UUID,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime
    ): List<ScheduleEntry>
}
