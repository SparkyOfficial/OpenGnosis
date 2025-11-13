package com.opengnosis.analytics.document

import com.opengnosis.domain.GradeType
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant
import java.util.UUID

@Document(indexName = "student_grades")
data class StudentGradesDocument(
    @Id
    val id: String,
    
    @Field(type = FieldType.Keyword)
    val studentId: String,
    
    @Field(type = FieldType.Keyword)
    val subjectId: String,
    
    @Field(type = FieldType.Integer)
    val gradeValue: Int,
    
    @Field(type = FieldType.Keyword)
    val gradeType: String,
    
    @Field(type = FieldType.Text)
    val comment: String? = null,
    
    @Field(type = FieldType.Keyword)
    val placedBy: String,
    
    @Field(type = FieldType.Date)
    val createdAt: Instant,
    
    @Field(type = FieldType.Date)
    val lastUpdated: Instant
)
