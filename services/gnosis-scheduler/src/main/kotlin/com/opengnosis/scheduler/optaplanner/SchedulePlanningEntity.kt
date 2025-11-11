package com.opengnosis.scheduler.optaplanner

import org.optaplanner.core.api.domain.entity.PlanningEntity
import org.optaplanner.core.api.domain.variable.PlanningVariable
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@PlanningEntity
data class SchedulePlanningEntity(
    val id: UUID = UUID.randomUUID(),
    val classId: UUID,
    val subjectId: UUID,
    val teacherId: UUID,
    
    @PlanningVariable(valueRangeProviderRefs = ["classroomRange"])
    var classroomId: UUID? = null,
    
    @PlanningVariable(valueRangeProviderRefs = ["timeSlotRange"])
    var timeSlot: TimeSlot? = null
) {
    // No-arg constructor required by OptaPlanner
    constructor() : this(
        id = UUID.randomUUID(),
        classId = UUID.randomUUID(),
        subjectId = UUID.randomUUID(),
        teacherId = UUID.randomUUID(),
        classroomId = null,
        timeSlot = null
    )
}

data class TimeSlot(
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime
) {
    fun overlaps(other: TimeSlot): Boolean {
        return dayOfWeek == other.dayOfWeek &&
               startTime < other.endTime &&
               endTime > other.startTime
    }
}
