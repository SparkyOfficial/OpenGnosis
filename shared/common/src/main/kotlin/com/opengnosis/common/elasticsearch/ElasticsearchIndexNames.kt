package com.opengnosis.common.elasticsearch

/**
 * Elasticsearch index name constants for the OpenGnosis platform.
 * Index templates are configured in Kubernetes manifests.
 */
object ElasticsearchIndexNames {
    const val STUDENT_GRADES = "student-grades"
    const val STUDENT_ATTENDANCE = "student-attendance"
    const val CLASS_PERFORMANCE = "class-performance"
    const val STUDENTS = "students"
    
    /**
     * Get time-based index name with current date suffix.
     * Format: index-name-YYYY.MM
     */
    fun getTimeBasedIndexName(baseName: String): String {
        val now = java.time.LocalDate.now()
        return "$baseName-${now.year}.${String.format("%02d", now.monthValue)}"
    }
}
