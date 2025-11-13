package com.opengnosis.analytics.service

import com.opengnosis.analytics.document.StudentSearchDocument
import com.opengnosis.analytics.repository.StudentSearchRepository
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val studentSearchRepository: StudentSearchRepository,
    private val elasticsearchOperations: ElasticsearchOperations
) {
    
    fun searchStudents(searchTerm: String): List<StudentSearchDocument> {
        if (searchTerm.isBlank()) {
            return emptyList()
        }
        
        return studentSearchRepository.searchStudents(searchTerm)
    }
    
    fun searchStudentsByEmail(email: String): StudentSearchDocument? {
        return studentSearchRepository.findByEmail(email)
    }
    
    fun searchStudentById(studentId: String): StudentSearchDocument? {
        return studentSearchRepository.findByStudentId(studentId)
    }
    
    fun advancedStudentSearch(
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        classId: String? = null
    ): List<StudentSearchDocument> {
        val criteria = mutableListOf<Criteria>()
        
        firstName?.let {
            if (it.isNotBlank()) {
                criteria.add(Criteria.where("firstName").contains(it))
            }
        }
        
        lastName?.let {
            if (it.isNotBlank()) {
                criteria.add(Criteria.where("lastName").contains(it))
            }
        }
        
        email?.let {
            if (it.isNotBlank()) {
                criteria.add(Criteria.where("email").contains(it))
            }
        }
        
        classId?.let {
            if (it.isNotBlank()) {
                criteria.add(Criteria.where("classIds").contains(it))
            }
        }
        
        if (criteria.isEmpty()) {
            return emptyList()
        }
        
        val query = CriteriaQuery(
            criteria.reduce { acc, c -> acc.and(c) }
        )
        
        val searchHits: SearchHits<StudentSearchDocument> = 
            elasticsearchOperations.search(query, StudentSearchDocument::class.java)
        
        return searchHits.map { it.content }.toList()
    }
    
    fun indexStudent(student: StudentSearchDocument) {
        studentSearchRepository.save(student)
    }
    
    fun updateStudentIndex(studentId: String, updates: Map<String, Any>) {
        val existingStudent = studentSearchRepository.findByStudentId(studentId)
        
        if (existingStudent != null) {
            val updatedStudent = existingStudent.copy(
                firstName = updates["firstName"] as? String ?: existingStudent.firstName,
                lastName = updates["lastName"] as? String ?: existingStudent.lastName,
                fullName = updates["fullName"] as? String ?: existingStudent.fullName,
                email = updates["email"] as? String ?: existingStudent.email,
                classIds = updates["classIds"] as? List<String> ?: existingStudent.classIds
            )
            
            studentSearchRepository.save(updatedStudent)
        }
    }
}
