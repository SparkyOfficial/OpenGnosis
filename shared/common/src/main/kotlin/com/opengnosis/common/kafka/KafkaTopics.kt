package com.opengnosis.common.kafka

/**
 * Kafka topic constants for the OpenGnosis platform.
 * All domain events are published to these topics with appropriate partitioning.
 */
object KafkaTopics {
    const val USER_EVENTS = "user-events"
    const val STRUCTURE_EVENTS = "structure-events"
    const val SCHEDULE_EVENTS = "schedule-events"
    const val JOURNAL_EVENTS = "journal-events"
    const val NOTIFICATION_EVENTS = "notification-events"
    const val DOMAIN_EVENTS = "domain-events"
    
    /**
     * Get the appropriate topic for a given event type.
     */
    fun getTopicForEventType(eventType: String): String {
        return when {
            eventType.contains("User", ignoreCase = true) -> USER_EVENTS
            eventType.contains("School", ignoreCase = true) ||
            eventType.contains("Class", ignoreCase = true) ||
            eventType.contains("Student", ignoreCase = true) ||
            eventType.contains("Teacher", ignoreCase = true) ||
            eventType.contains("Enrollment", ignoreCase = true) ||
            eventType.contains("Subject", ignoreCase = true) -> STRUCTURE_EVENTS
            eventType.contains("Schedule", ignoreCase = true) -> SCHEDULE_EVENTS
            eventType.contains("Grade", ignoreCase = true) ||
            eventType.contains("Attendance", ignoreCase = true) ||
            eventType.contains("Homework", ignoreCase = true) -> JOURNAL_EVENTS
            eventType.contains("Notification", ignoreCase = true) -> NOTIFICATION_EVENTS
            else -> DOMAIN_EVENTS
        }
    }
}
