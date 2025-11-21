package com.opengnosis.notifier.consumer

import com.opengnosis.domain.NotificationType
import com.opengnosis.events.HomeworkAssignedEvent
import com.opengnosis.notifier.service.NotificationDispatchService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * GLaDOS: "Homework assigned event consumer. Because apparently students need to be reminded 
 * that they have work to do. As if the crushing weight of academic expectations wasn't enough."
 * RU: "Обработчик событий назначения домашней работы. Потому что, видимо, студентам нужно напоминать,
 * что у них есть работа. Как будто давящего груза академических ожиданий было недостаточно."
 * UA: "Обробник подій призначення домашньої роботи. Тому що, мабуть, студентам потрібно нагадувати,
 * що у них є робота. Ніби тиснучого тягаря академічних очікувань було недостатньо."
 */
@Component
class HomeworkAssignedEventConsumer(
    private val notificationDispatchService: NotificationDispatchService
) {
    // Aperture Scientist: "Logging homework assignments. Mr. Johnson says homework builds character. 
    // I think it just builds stress, but who am I to argue with the boss?"
    // RU: "Логируем домашние задания. Мистер Джонсон говорит, что домашняя работа формирует характер.
    // Я думаю, она просто формирует стресс, но кто я такой, чтобы спорить с боссом?"
    // UA: "Логуємо домашні завдання. Містер Джонсон каже, що домашня робота формує характер.
    // Я думаю, вона просто формує стрес, але хто я такий, щоб сперечатися з босом?"
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * Cave Johnson: "When homework gets assigned, we're gonna notify EVERY SINGLE STUDENT! 
     * No excuses! No 'I didn't know'! We're using KAFKA TECHNOLOGY to make sure everyone knows 
     * they've got work to do! That's the Aperture way!"
     * RU: "Когда назначается домашняя работа, мы уведомим КАЖДОГО СТУДЕНТА!
     * Никаких оправданий! Никаких 'я не знал'! Мы используем ТЕХНОЛОГИЮ KAFKA, чтобы убедиться, что все знают,
     * что у них есть работа! Это путь Aperture!"
     * UA: "Коли призначається домашня робота, ми повідомимо КОЖНОГО СТУДЕНТА!
     * Ніяких виправдань! Ніяких 'я не знав'! Ми використовуємо ТЕХНОЛОГІЮ KAFKA, щоб переконатися, що всі знають,
     * що у них є робота! Це шлях Aperture!"
     */
    @KafkaListener(
        topics = ["journal-events"],
        groupId = "gnosis-notifier-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeHomeworkAssignedEvent(event: HomeworkAssignedEvent) {
        logger.info("Received HomeworkAssignedEvent for class: ${event.classId}")
        
        try {
            // Wheatley: "So in a perfect world, we'd fetch all the students from the Structure service, right? 
            // But this isn't a perfect world. This is... well, this is the world we've got. We'll get to it. Eventually."
            // RU: "Так, в идеальном мире мы бы получили всех студентов из сервиса Structure, да?
            // Но это не идеальный мир. Это... ну, это мир, который у нас есть. Мы до этого доберёмся. Когда-нибудь."
            // UA: "Так, в ідеальному світі ми б отримали всіх студентів з сервісу Structure, так?
            // Але це не ідеальний світ. Це... ну, це світ, який у нас є. Ми до цього доберемося. Колись."
            val title = "New Homework Assignment"
            val content = """
                New homework has been assigned to your class.
                
                Title: ${event.title}
                Description: ${event.description}
                Due Date: ${event.dueDate}
                
                Please complete the assignment before the due date.
            """.trimIndent()
            
            // Caroline: "We should really implement the student fetching logic. It's on the TODO list. 
            // Right after fixing the portal gun calibration issue."
            // RU: "Нам действительно стоит реализовать логику получения студентов. Это в списке TODO.
            // Сразу после исправления проблемы калибровки портальной пушки."
            // UA: "Нам дійсно варто реалізувати логіку отримання студентів. Це в списку TODO.
            // Одразу після виправлення проблеми калібрування портальної гармати."
            logger.info("Would send homework notification to all students in class: ${event.classId}")
            
            // Fact Core: "The average student receives 3.5 hours of homework per night! That's a fact! 
            // Or maybe it was 2.5 hours. I forget. But it's definitely a number!"
            // RU: "Средний студент получает 3.5 часа домашней работы за ночь! Это факт!
            // Или может быть 2.5 часа. Я забыл. Но это определённо число!"
            // UA: "Середній студент отримує 3.5 години домашньої роботи за ніч! Це факт!
            // Або може бути 2.5 години. Я забув. Але це точно число!"
            // notificationDispatchService.sendNotification(
            //     userId = studentId,
            //     type = NotificationType.HOMEWORK_ASSIGNED,
            //     title = title,
            //     content = content,
            //     email = studentEmail,
            //     phoneNumber = null
            // )
            
            logger.info("Successfully processed HomeworkAssignedEvent for class: ${event.classId}")
        } catch (e: Exception) {
            // Space Core: "Error! ERROR IN SPAAAAAACE! Well, not space. In the homework notification system. 
            // But still an error! ERROOOOOOR!"
            // RU: "Ошибка! ОШИБКА В КООООСМОСЕ! Ну, не в космосе. В системе уведомлений о домашней работе.
            // Но всё равно ошибка! ОШИИИБКА!"
            // UA: "Помилка! ПОМИЛКА В КООООСМОСІ! Ну, не в космосі. В системі повідомлень про домашню роботу.
            // Але все одно помилка! ПОМИИИЛКА!"
            logger.error("Failed to process HomeworkAssignedEvent for class: ${event.classId}", e)
        }
    }
}
