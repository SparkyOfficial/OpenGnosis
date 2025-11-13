package com.opengnosis.analytics.repository

import com.opengnosis.analytics.document.StudentGradesDocument
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface StudentGradesElasticsearchRepository : ElasticsearchRepository<StudentGradesDocument, String> {
    
    fun findByStudentId(studentId: String): List<StudentGradesDocument>
    
    fun findByStudentIdAndSubjectId(studentId: String, subjectId: String): List<StudentGradesDocument>
}
