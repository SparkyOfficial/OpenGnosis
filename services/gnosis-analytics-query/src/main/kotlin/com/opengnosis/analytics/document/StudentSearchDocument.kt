package com.opengnosis.analytics.document

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.util.UUID

@Document(indexName = "students")
data class StudentSearchDocument(
    @Id
    val id: String,
    
    @Field(type = FieldType.Text, analyzer = "standard")
    val firstName: String,
    
    @Field(type = FieldType.Text, analyzer = "standard")
    val lastName: String,
    
    @Field(type = FieldType.Text, analyzer = "standard")
    val fullName: String,
    
    @Field(type = FieldType.Keyword)
    val email: String,
    
    @Field(type = FieldType.Keyword)
    val studentId: String,
    
    @Field(type = FieldType.Keyword)
    val classIds: List<String> = emptyList()
)
