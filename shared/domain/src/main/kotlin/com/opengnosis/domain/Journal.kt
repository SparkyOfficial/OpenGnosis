package com.opengnosis.domain

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

sealed class JournalCommand {
    abstract val id: UUID
    abstract val timestamp: Instant
    abstract val issuedBy: UUID
}

data class PlaceGradeCommand(
    override val id: UUID,
    override val timestamp: Instant,
    override val issuedBy: UUID,
    val studentId: UUID,
    val subjectId: UUID,
    val gradeValue: Int,
    val gradeType: GradeType,
    val comment: String?
) : JournalCommand()

data class MarkAttendanceCommand(
    override val id: UUID,
    override val timestamp: Instant,
    override val issuedBy: UUID,
    val studentId: UUID,
    val classId: UUID,
    val date: LocalDate,
    val lessonNumber: Int,
    val status: AttendanceStatus
) : JournalCommand()

data class AssignHomeworkCommand(
    override val id: UUID,
    override val timestamp: Instant,
    override val issuedBy: UUID,
    val classId: UUID,
    val subjectId: UUID,
    val title: String,
    val description: String,
    val dueDate: LocalDate
) : JournalCommand()

enum class GradeType {
    EXAM, QUIZ, HOMEWORK, CLASSWORK, FINAL
}

enum class AttendanceStatus {
    PRESENT, ABSENT, LATE, EXCUSED
}
