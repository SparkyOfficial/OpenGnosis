package com.opengnosis.scheduler.optaplanner

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore
import org.optaplanner.core.api.score.stream.Constraint
import org.optaplanner.core.api.score.stream.ConstraintFactory
import org.optaplanner.core.api.score.stream.ConstraintProvider
import org.optaplanner.core.api.score.stream.Joiners

class ScheduleConstraintProvider : ConstraintProvider {
    
    override fun defineConstraints(constraintFactory: ConstraintFactory): Array<Constraint> {
        return arrayOf(
            // Hard constraints
            teacherConflict(constraintFactory),
            classroomConflict(constraintFactory),
            classConflict(constraintFactory),
            teacherAvailability(constraintFactory),
            
            // Soft constraints
            minimizeTeacherIdleTime(constraintFactory),
            preferredClassrooms(constraintFactory)
        )
    }
    
    // Hard constraint: Teacher cannot teach two classes at the same time
    private fun teacherConflict(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(SchedulePlanningEntity::class.java)
            .join(
                SchedulePlanningEntity::class.java,
                Joiners.equal { it.teacherId },
                Joiners.lessThan { it.id }
            )
            .filter { entry1, entry2 ->
                entry1.timeSlot != null && entry2.timeSlot != null &&
                entry1.timeSlot!!.overlaps(entry2.timeSlot!!)
            }
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Teacher conflict")
    }
    
    // Hard constraint: Classroom cannot be used by two classes at the same time
    private fun classroomConflict(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(SchedulePlanningEntity::class.java)
            .join(
                SchedulePlanningEntity::class.java,
                Joiners.equal { it.classroomId },
                Joiners.lessThan { it.id }
            )
            .filter { entry1, entry2 ->
                entry1.timeSlot != null && entry2.timeSlot != null &&
                entry1.classroomId != null &&
                entry1.timeSlot!!.overlaps(entry2.timeSlot!!)
            }
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Classroom conflict")
    }
    
    // Hard constraint: Class cannot have two lessons at the same time
    private fun classConflict(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(SchedulePlanningEntity::class.java)
            .join(
                SchedulePlanningEntity::class.java,
                Joiners.equal { it.classId },
                Joiners.lessThan { it.id }
            )
            .filter { entry1, entry2 ->
                entry1.timeSlot != null && entry2.timeSlot != null &&
                entry1.timeSlot!!.overlaps(entry2.timeSlot!!)
            }
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Class conflict")
    }
    
    // Hard constraint: Teacher must be available at the scheduled time
    private fun teacherAvailability(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(SchedulePlanningEntity::class.java)
            .filter { entry ->
                entry.timeSlot != null
            }
            .penalize(HardSoftScore.ONE_HARD) { entry ->
                // This is a simplified check - in real implementation,
                // we would check against actual teacher availability data
                0
            }
            .asConstraint("Teacher availability")
    }
    
    // Soft constraint: Minimize gaps in teacher's schedule
    private fun minimizeTeacherIdleTime(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(SchedulePlanningEntity::class.java)
            .join(
                SchedulePlanningEntity::class.java,
                Joiners.equal { it.teacherId }
            )
            .filter { entry1, entry2 ->
                entry1.timeSlot != null && entry2.timeSlot != null &&
                entry1.timeSlot!!.dayOfWeek == entry2.timeSlot!!.dayOfWeek &&
                entry1.id != entry2.id
            }
            .penalize(HardSoftScore.ONE_SOFT) { entry1, entry2 ->
                val gap = calculateGapMinutes(entry1.timeSlot!!, entry2.timeSlot!!)
                if (gap in 1..120) gap else 0 // Penalize gaps between 1 and 120 minutes
            }
            .asConstraint("Minimize teacher idle time")
    }
    
    // Soft constraint: Prefer certain classrooms for certain subjects (placeholder)
    private fun preferredClassrooms(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(SchedulePlanningEntity::class.java)
            .filter { entry ->
                entry.classroomId != null
            }
            .reward(HardSoftScore.ONE_SOFT)
            .asConstraint("Preferred classrooms")
    }
    
    private fun calculateGapMinutes(slot1: TimeSlot, slot2: TimeSlot): Int {
        if (slot1.dayOfWeek != slot2.dayOfWeek) return 0
        
        val (earlier, later) = if (slot1.endTime <= slot2.startTime) {
            slot1 to slot2
        } else if (slot2.endTime <= slot1.startTime) {
            slot2 to slot1
        } else {
            return 0 // Overlapping
        }
        
        val gapStart = earlier.endTime
        val gapEnd = later.startTime
        
        return ((gapEnd.toSecondOfDay() - gapStart.toSecondOfDay()) / 60)
    }
}
