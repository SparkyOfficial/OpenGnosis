package com.opengnosis.analytics.document

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Document(indexName = "student_attendance")
data class StudentAttendanceDocument(
    @Id
    val id: String,
    
    @Field(type = FieldType.Keyword)
    val studentId: String,
    
    @Field(type = FieldType.Keyword)
    val classId: String,
    
    @Field(type = FieldType.Date)
    val date: LocalDate,
    
    @Field(type = FieldType.Integer)
    val lessonNumber: Int,
    
    @Field(type = FieldType.Keyword)
    val status: String,
    
    @Field(type = FieldType.Keyword)
    val markedBy: String,
    
    @Field(type = FieldType.Date)
    val createdAt: Instant
)
