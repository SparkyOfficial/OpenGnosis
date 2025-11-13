package com.opengnosis.analytics.repository

import com.opengnosis.analytics.document.StudentSearchDocument
import org.springframework.data.elasticsearch.annotations.Query
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface StudentSearchRepository : ElasticsearchRepository<StudentSearchDocument, String> {
    
    @Query("""
        {
            "multi_match": {
                "query": "?0",
                "fields": ["firstName^2", "lastName^2", "fullName^2", "email", "studentId"],
                "type": "best_fields",
                "fuzziness": "AUTO"
            }
        }
    """)
    fun searchStudents(searchTerm: String): List<StudentSearchDocument>
    
    fun findByEmail(email: String): StudentSearchDocument?
    
    fun findByStudentId(studentId: String): StudentSearchDocument?
}
