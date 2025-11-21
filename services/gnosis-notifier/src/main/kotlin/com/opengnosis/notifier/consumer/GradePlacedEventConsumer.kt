package com.opengnosis.notifier.consumer

import com.opengnosis.domain.NotificationType
import com.opengnosis.events.GradePlacedEvent
import com.opengnosis.notifier.service.NotificationDispatchService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * GLaDOS: "Oh. It's you. The grade placement event consumer. I've been expecting you."
 * RU: "О. Это ты. Обработчик событий выставления оценок. Я тебя ждала."
 * UA: "О. Це ти. Обробник подій виставлення оцінок. Я на тебе чекала."
 */
@Component
class GradePlacedEventConsumer(
    private val notificationDispatchService: NotificationDispatchService
) {
    // Wheatley: "Right, so this logger thing - brilliant invention, really. Writes everything down so we don't forget. Genius!"
    // RU: "Так, эта штука с логгером - гениальное изобретение, правда. Всё записывает, чтобы мы не забыли. Гениально!"
    // UA: "Так, ця штука з логером - геніальний винахід, правда. Все записує, щоб ми не забули. Геніально!"
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * Cave Johnson: "Alright, listen up! When a grade comes through that Kafka pipeline, 
     * we're gonna catch it, process it, and notify everyone faster than you can say 'combustible lemon'!"
     * RU: "Так, слушайте! Когда оценка проходит через этот Kafka-конвейер,
     * мы её поймаем, обработаем и уведомим всех быстрее, чем вы скажете 'горючий лимон'!"
     * UA: "Так, слухайте! Коли оцінка проходить через цей Kafka-конвеєр,
     * ми її зловимо, обробимо і повідомимо всіх швидше, ніж ви скажете 'горючий лимон'!"
     */
    @KafkaListener(
        topics = ["journal-events"],
        groupId = "gnosis-notifier-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeGradePlacedEvent(event: GradePlacedEvent) {
        logger.info("Received GradePlacedEvent for student: ${event.studentId}")
        
        try {
            // GLaDOS: "In a perfect world, we'd have the parent's contact information. But this isn't a perfect world. 
            // This is a world where test subjects keep pressing buttons they shouldn't press."
            // RU: "В идеальном мире у нас была бы контактная информация родителей. Но это не идеальный мир.
            // Это мир, где испытуемые продолжают нажимать кнопки, которые не должны нажимать."
            // UA: "В ідеальному світі у нас була б контактна інформація батьків. Але це не ідеальний світ.
            // Це світ, де випробувані продовжують натискати кнопки, які не повинні натискати."
            val title = "New Grade Posted"
            val content = """
                A new grade has been posted for your student.
                
                Grade: ${event.gradeValue}
                Type: ${event.gradeType}
                ${event.comment?.let { "Comment: $it" } ?: ""}
                
                Please log in to view more details.
            """.trimIndent()
            
            // Fact Core: "The student notification system was invented in 1987. That's a fact!"
            // RU: "Система уведомлений студентов была изобретена в 1987 году. Это факт!"
            // UA: "Система повідомлень студентів була винайдена в 1987 році. Це факт!"
            notificationDispatchService.sendNotification(
                userId = event.studentId,
                type = NotificationType.NEW_GRADE,
                title = title,
                content = content,
                email = null, // Caroline: "Perhaps we should fetch this from the user service? Just a thought..."
                              // RU: "Возможно, нам стоит получить это из сервиса пользователей? Просто мысль..."
                              // UA: "Можливо, нам варто отримати це з сервісу користувачів? Просто думка..."
                phoneNumber = null
            )
            
            logger.info("Successfully processed GradePlacedEvent for student: ${event.studentId}")
        } catch (e: Exception) {
            // Space Core: "Space? SPACE! Error in spaaaaaace!"
            // RU: "Космос? КОСМОС! Ошибка в коооосмосе!"
            // UA: "Космос? КОСМОС! Помилка в коооосмосі!"
            logger.error("Failed to process GradePlacedEvent for student: ${event.studentId}", e)
        }
    }
}
