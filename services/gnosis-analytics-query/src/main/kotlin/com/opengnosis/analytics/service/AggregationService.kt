package com.opengnosis.analytics.service

import com.opengnosis.analytics.document.StudentGradesDocument
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class AggregationService(
    private val elasticsearchOperations: ElasticsearchOperations
) {
    
    fun getGradeDistributionBySubject(subjectId: String): Map<Int, Long> {
        val criteria = Criteria.where("subjectId").`is`(subjectId)
        val query = CriteriaQuery(criteria)
        
        val searchHits: SearchHits<StudentGradesDocument> = 
            elasticsearchOperations.search(query, StudentGradesDocument::class.java)
        
        return searchHits
            .map { it.content.gradeValue }
            .groupBy { it }
            .mapValues { it.value.size.toLong() }
    }
    
    fun getGradeDistributionByStudent(studentId: String): Map<Int, Long> {
        val criteria = Criteria.where("studentId").`is`(studentId)
        val query = CriteriaQuery(criteria)
        
        val searchHits: SearchHits<StudentGradesDocument> = 
            elasticsearchOperations.search(query, StudentGradesDocument::class.java)
        
        return searchHits
            .map { it.content.gradeValue }
            .groupBy { it }
            .mapValues { it.value.size.toLong() }
    }
    
    fun getGradeTrendsByStudent(studentId: String, subjectId: String): List<GradeTrend> {
        val criteria = Criteria.where("studentId").`is`(studentId)
            .and("subjectId").`is`(subjectId)
        val query = CriteriaQuery(criteria)
        
        val searchHits: SearchHits<StudentGradesDocument> = 
            elasticsearchOperations.search(query, StudentGradesDocument::class.java)
        
        return searchHits
            .map { hit ->
                val doc = hit.content
                GradeTrend(
                    timestamp = doc.createdAt,
                    gradeValue = doc.gradeValue,
                    gradeType = doc.gradeType
                )
            }
            .sortedBy { it.timestamp }
    }
    
    fun getAverageGradesBySubject(): Map<String, Double> {
        val searchHits = elasticsearchOperations.search(
            CriteriaQuery(Criteria.where("gradeValue").exists()),
            StudentGradesDocument::class.java
        )
        
        return searchHits.searchHits
            .map { it.content }
            .groupBy { it.subjectId }
            .mapValues { (_, grades) ->
                grades.map { it.gradeValue }.average()
            }
    }
    
    fun getDashboardStatistics(): DashboardStatistics {
        val searchHits = elasticsearchOperations.search(
            CriteriaQuery(Criteria.where("gradeValue").exists()),
            StudentGradesDocument::class.java
        )
        
        val allGrades = searchHits.searchHits.map { it.content }
        
        val totalGrades = allGrades.size
        val averageGrade = if (allGrades.isNotEmpty()) {
            allGrades.map { it.gradeValue }.average()
        } else {
            0.0
        }
        
        val uniqueStudents = allGrades.map { it.studentId }.distinct().size
        val uniqueSubjects = allGrades.map { it.subjectId }.distinct().size
        
        val gradeDistribution = allGrades
            .groupBy { it.gradeValue }
            .mapValues { it.value.size }
        
        return DashboardStatistics(
            totalGrades = totalGrades,
            averageGrade = averageGrade,
            uniqueStudents = uniqueStudents,
            uniqueSubjects = uniqueSubjects,
            gradeDistribution = gradeDistribution
        )
    }
}

data class GradeTrend(
    val timestamp: Instant,
    val gradeValue: Int,
    val gradeType: String
)

data class DashboardStatistics(
    val totalGrades: Int,
    val averageGrade: Double,
    val uniqueStudents: Int,
    val uniqueSubjects: Int,
    val gradeDistribution: Map<Int, Int>
)
