package com.opengnosis.analytics.service

import com.opengnosis.analytics.document.StudentGradesDocument
import com.opengnosis.analytics.entity.StudentGradesEntity
import com.opengnosis.analytics.repository.StudentGradesElasticsearchRepository
import com.opengnosis.analytics.repository.StudentGradesRepository
import com.opengnosis.events.GradePlacedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GradesReadModelService(
    private val gradesRepository: StudentGradesRepository,
    private val gradesElasticsearchRepository: StudentGradesElasticsearchRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @Transactional
    fun handleGradePlacedEvent(event: GradePlacedEvent) {
        logger.info("Updating read model for GradePlacedEvent: studentId=${event.studentId}, subjectId=${event.subjectId}")
        
        // Save to PostgreSQL
        val gradeEntity = StudentGradesEntity(
            studentId = event.studentId,
            subjectId = event.subjectId,
            gradeValue = event.gradeValue,
            gradeType = event.gradeType,
            comment = event.comment,
            placedBy = event.placedBy,
            createdAt = event.timestamp,
            lastUpdated = Instant.now()
        )
        
        val savedEntity = gradesRepository.save(gradeEntity)
        logger.info("Saved grade to PostgreSQL: id=${savedEntity.id}")
        
        // Index in Elasticsearch
        val gradeDocument = StudentGradesDocument(
            id = savedEntity.id.toString(),
            studentId = event.studentId.toString(),
            subjectId = event.subjectId.toString(),
            gradeValue = event.gradeValue,
            gradeType = event.gradeType.name,
            comment = event.comment,
            placedBy = event.placedBy.toString(),
            createdAt = event.timestamp,
            lastUpdated = Instant.now()
        )
        
        gradesElasticsearchRepository.save(gradeDocument)
        logger.info("Indexed grade in Elasticsearch: id=${gradeDocument.id}")
    }
    
    fun calculateAverageGrade(studentId: java.util.UUID, subjectId: java.util.UUID): Double {
        return gradesRepository.calculateAverageGrade(studentId, subjectId) ?: 0.0
    }
    
    fun getGradeStatistics(studentId: java.util.UUID, subjectId: java.util.UUID): GradeStatistics {
        val grades = gradesRepository.findByStudentIdAndSubjectId(studentId, subjectId)
        
        if (grades.isEmpty()) {
            return GradeStatistics(
                average = 0.0,
                count = 0,
                min = 0,
                max = 0
            )
        }
        
        val gradeValues = grades.map { it.gradeValue }
        return GradeStatistics(
            average = gradeValues.average(),
            count = grades.size,
            min = gradeValues.minOrNull() ?: 0,
            max = gradeValues.maxOrNull() ?: 0
        )
    }
}

data class GradeStatistics(
    val average: Double,
    val count: Int,
    val min: Int,
    val max: Int
)
