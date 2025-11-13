package com.opengnosis.analytics.service

import com.opengnosis.analytics.entity.StudentEnrollmentEntity
import com.opengnosis.analytics.repository.StudentEnrollmentRepository
import com.opengnosis.events.StudentEnrolledEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EnrollmentReadModelService(
    private val enrollmentRepository: StudentEnrollmentRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @Transactional
    fun handleStudentEnrolledEvent(event: StudentEnrolledEvent) {
        logger.info("Updating read model for StudentEnrolledEvent: studentId=${event.studentId}, classId=${event.classId}")
        
        // Check if enrollment already exists
        val existingEnrollment = enrollmentRepository.findByStudentIdAndClassId(
            event.studentId,
            event.classId
        )
        
        if (existingEnrollment != null) {
            logger.info("Enrollment already exists: id=${existingEnrollment.id}")
            return
        }
        
        // Create new enrollment
        val enrollmentEntity = StudentEnrollmentEntity(
            studentId = event.studentId,
            classId = event.classId,
            enrollmentDate = event.enrollmentDate,
            isActive = true
        )
        
        val savedEntity = enrollmentRepository.save(enrollmentEntity)
        logger.info("Saved enrollment to PostgreSQL: id=${savedEntity.id}")
    }
}
