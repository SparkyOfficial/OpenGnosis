package com.opengnosis.scheduler.optaplanner

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty
import org.optaplanner.core.api.domain.solution.PlanningScore
import org.optaplanner.core.api.domain.solution.PlanningSolution
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore
import java.util.UUID

@PlanningSolution
data class ScheduleSolution(
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "classroomRange")
    var classrooms: List<UUID> = emptyList(),
    
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "timeSlotRange")
    var timeSlots: List<TimeSlot> = emptyList(),
    
    @ProblemFactCollectionProperty
    var teacherAvailabilities: List<TeacherAvailabilityConstraint> = emptyList(),
    
    @PlanningEntityCollectionProperty
    var scheduleEntries: List<SchedulePlanningEntity> = emptyList(),
    
    @PlanningScore
    var score: HardSoftScore? = null
) {
    // No-arg constructor required by OptaPlanner
    constructor() : this(
        classrooms = emptyList(),
        timeSlots = emptyList(),
        teacherAvailabilities = emptyList(),
        scheduleEntries = emptyList(),
        score = null
    )
}

data class TeacherAvailabilityConstraint(
    val teacherId: UUID,
    val availableTimeSlots: List<TimeSlot>
)
