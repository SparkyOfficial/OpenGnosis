package com.opengnosis.analytics.service

import com.opengnosis.analytics.document.StudentAttendanceDocument
import com.opengnosis.analytics.entity.StudentAttendanceEntity
import com.opengnosis.analytics.repository.StudentAttendanceRepository
import com.opengnosis.domain.AttendanceStatus
import com.opengnosis.events.AttendanceMarkedEvent
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AttendanceReadModelService(
    private val attendanceRepository: StudentAttendanceRepository,
    private val elasticsearchOperations: ElasticsearchOperations
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @Transactional
    fun handleAttendanceMarkedEvent(event: AttendanceMarkedEvent) {
        logger.info("Updating read model for AttendanceMarkedEvent: studentId=${event.studentId}, classId=${event.classId}")
        
        // Save to PostgreSQL
        val attendanceEntity = StudentAttendanceEntity(
            studentId = event.studentId,
            classId = event.classId,
            date = event.date,
            lessonNumber = event.lessonNumber,
            status = event.status,
            markedBy = event.markedBy,
            createdAt = event.timestamp
        )
        
        val savedEntity = attendanceRepository.save(attendanceEntity)
        logger.info("Saved attendance to PostgreSQL: id=${savedEntity.id}")
        
        // Index in Elasticsearch
        val attendanceDocument = StudentAttendanceDocument(
            id = savedEntity.id.toString(),
            studentId = event.studentId.toString(),
            classId = event.classId.toString(),
            date = event.date,
            lessonNumber = event.lessonNumber,
            status = event.status.name,
            markedBy = event.markedBy.toString(),
            createdAt = event.timestamp
        )
        
        elasticsearchOperations.save(attendanceDocument)
        logger.info("Indexed attendance in Elasticsearch: id=${attendanceDocument.id}")
    }
    
    fun calculateAttendanceRate(studentId: UUID, classId: UUID): Double {
        val totalLessons = attendanceRepository.countTotalLessons(studentId, classId)
        
        if (totalLessons == 0L) {
            return 0.0
        }
        
        val presentCount = attendanceRepository.countByStudentIdAndClassIdAndStatus(
            studentId, classId, AttendanceStatus.PRESENT
        )
        
        return (presentCount.toDouble() / totalLessons.toDouble()) * 100.0
    }
    
    fun getAttendanceStatistics(studentId: UUID, classId: UUID): AttendanceStatistics {
        val totalLessons = attendanceRepository.countTotalLessons(studentId, classId)
        val presentCount = attendanceRepository.countByStudentIdAndClassIdAndStatus(
            studentId, classId, AttendanceStatus.PRESENT
        )
        val absentCount = attendanceRepository.countByStudentIdAndClassIdAndStatus(
            studentId, classId, AttendanceStatus.ABSENT
        )
        val lateCount = attendanceRepository.countByStudentIdAndClassIdAndStatus(
            studentId, classId, AttendanceStatus.LATE
        )
        val excusedCount = attendanceRepository.countByStudentIdAndClassIdAndStatus(
            studentId, classId, AttendanceStatus.EXCUSED
        )
        
        val attendanceRate = if (totalLessons > 0) {
            (presentCount.toDouble() / totalLessons.toDouble()) * 100.0
        } else {
            0.0
        }
        
        return AttendanceStatistics(
            totalLessons = totalLessons.toInt(),
            presentCount = presentCount.toInt(),
            absentCount = absentCount.toInt(),
            lateCount = lateCount.toInt(),
            excusedCount = excusedCount.toInt(),
            attendanceRate = attendanceRate
        )
    }
}

data class AttendanceStatistics(
    val totalLessons: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val excusedCount: Int,
    val attendanceRate: Double
)
