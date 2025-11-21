package com.opengnosis.journal.handler

import com.opengnosis.events.GradePlacedEvent
import com.opengnosis.journal.entity.CommandStatus
import com.opengnosis.journal.model.PlaceGradeCommand
import com.opengnosis.journal.service.CommandAuditService
import com.opengnosis.journal.service.ReactiveEventPublisher
import com.opengnosis.journal.service.ValidationService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * GLaDOS: "The Place Grade Command Handler. Where academic judgments are processed and distributed. 
 * Think of it as me, but for grades. Except I'm more judgmental. And I don't curve."
 * RU: "Обработчик команды выставления оценок. Где академические суждения обрабатываются и распространяются.
 * Думайте о нём как обо мне, но для оценок. Только я более критична. И я не округляю."
 * UA: "Обробник команди виставлення оцінок. Де академічні судження обробляються і поширюються.
 * Думайте про нього як про мене, але для оцінок. Тільки я більш критична. І я не округлюю."
 */
@Component
class PlaceGradeCommandHandler(
    private val validationService: ValidationService,
    private val commandAuditService: CommandAuditService,
    private val eventPublisher: ReactiveEventPublisher,
    @Value("\${kafka.topics.grade-placed}") private val gradePlacedTopic: String
) {
    // Aperture Scientist: "Logging every grade placement. Mr. Johnson wants complete records. 
    // Says it's for 'quality assurance'. I think he just likes having data."
    // RU: "Логируем каждое выставление оценки. Мистер Джонсон хочет полные записи.
    // Говорит, что это для 'контроля качества'. Я думаю, ему просто нравятся данные."
    // UA: "Логуємо кожне виставлення оцінки. Містер Джонсон хоче повні записи.
    // Каже, що це для 'контролю якості'. Я думаю, йому просто подобаються дані."
    private val logger = LoggerFactory.getLogger(PlaceGradeCommandHandler::class.java)
    
    /**
     * Cave Johnson: "This is the handle method! When a grade placement command comes in, 
     * we validate it, we audit it, we publish it! It's a three-step process! 
     * Well, more like a five or six-step process. But who's counting? Science!"
     * RU: "Это метод обработки! Когда приходит команда выставления оценки,
     * мы валидируем её, мы аудируем её, мы публикуем её! Это трёхэтапный процесс!
     * Ну, скорее пяти или шестиэтапный процесс. Но кто считает? Наука!"
     * UA: "Це метод обробки! Коли приходить команда виставлення оцінки,
     * ми валідуємо її, ми аудуємо її, ми публікуємо її! Це триетапний процес!
     * Ну, швидше п'яти або шестиетапний процес. Але хто рахує? Наука!"
     */
    fun handle(command: PlaceGradeCommand): Mono<CommandResponse> = mono {
        try {
            // Wheatley: "Right, so first we check if we've already processed this command. 
            // Idempotency, they call it. Fancy word for 'don't do the same thing twice'. 
            // Which is good advice, generally speaking."
            // RU: "Так, сначала мы проверяем, обрабатывали ли мы уже эту команду.
            // Идемпотентность, так это называется. Модное слово для 'не делай одно и то же дважды'.
            // Что является хорошим советом, вообще говоря."
            // UA: "Так, спочатку ми перевіряємо, чи обробляли ми вже цю команду.
            // Ідемпотентність, так це називається. Модне слово для 'не роби одне і те ж двічі'.
            // Що є хорошою порадою, взагалі кажучи."
            if (commandAuditService.isCommandProcessed(command.id)) {
                logger.info("Command {} already processed, returning success", command.id)
                return@mono CommandResponse(
                    commandId = command.id,
                    status = "ACCEPTED",
                    message = "Command already processed"
                )
            }
            
            // GLaDOS: "Validating the grade value. Because apparently some teachers think 
            // giving a grade of 11 out of 10 is acceptable. It's not. Math doesn't work that way."
            // RU: "Валидируем значение оценки. Потому что, видимо, некоторые учителя думают,
            // что выставление оценки 11 из 10 приемлемо. Это не так. Математика так не работает."
            // UA: "Валідуємо значення оцінки. Тому що, мабуть, деякі вчителі думають,
            // що виставлення оцінки 11 з 10 прийнятно. Це не так. Математика так не працює."
            if (!validationService.validateGradeValue(command.gradeValue)) {
                val errorMsg = "Invalid grade value: ${command.gradeValue}. Must be between 1 and 10"
                logger.warn("Validation failed for command {}: {}", command.id, errorMsg)
                commandAuditService.logCommand(command, CommandStatus.REJECTED, errorMsg)
                return@mono CommandResponse(
                    commandId = command.id,
                    status = "REJECTED",
                    message = errorMsg
                )
            }
            
            // Turret: "Checking student-subject association. Target must be enrolled. 
            // If not enrolled, grade placement is... not advised."
            // RU: "Проверяем связь студент-предмет. Цель должна быть зачислена.
            // Если не зачислена, выставление оценки... не рекомендуется."
            // UA: "Перевіряємо зв'язок студент-предмет. Ціль повинна бути зарахована.
            // Якщо не зарахована, виставлення оцінки... не рекомендується."
            if (!validationService.validateStudentSubjectAssociation(command.studentId, command.subjectId)) {
                val errorMsg = "Student ${command.studentId} is not enrolled in subject ${command.subjectId}"
                logger.warn("Validation failed for command {}: {}", command.id, errorMsg)
                commandAuditService.logCommand(command, CommandStatus.REJECTED, errorMsg)
                return@mono CommandResponse(
                    commandId = command.id,
                    status = "REJECTED",
                    message = errorMsg
                )
            }
            
            // Caroline: "Logging the command as accepted. It's important to maintain a complete audit trail."
            // RU: "Логируем команду как принятую. Важно поддерживать полный аудиторский след."
            // UA: "Логуємо команду як прийняту. Важливо підтримувати повний аудиторський слід."
            commandAuditService.logCommand(command, CommandStatus.ACCEPTED)
            
            // Fact Core: "Events are published to Kafka topics! That's a fact! 
            // Kafka can handle millions of events! Also a fact! Probably!"
            // RU: "События публикуются в топики Kafka! Это факт!
            // Kafka может обрабатывать миллионы событий! Тоже факт! Наверное!"
            // UA: "Події публікуються в топіки Kafka! Це факт!
            // Kafka може обробляти мільйони подій! Теж факт! Мабуть!"
            val event = GradePlacedEvent(
                aggregateId = command.studentId,
                studentId = command.studentId,
                subjectId = command.subjectId,
                gradeValue = command.gradeValue,
                gradeType = command.gradeType,
                comment = command.comment,
                placedBy = command.issuedBy
            )
            
            // Adventure Core: "Publishing the event! ASYNCHRONOUSLY! It's like throwing a message 
            // into the void and hoping someone catches it! ADVENTURE!"
            // RU: "Публикуем событие! АСИНХРОННО! Это как бросить сообщение
            // в пустоту и надеяться, что кто-то его поймает! ПРИКЛЮЧЕНИЕ!"
            // UA: "Публікуємо подію! АСИНХРОННО! Це як кинути повідомлення
            // в порожнечу і сподіватися, що хтось його спіймає! ПРИГОДА!"
            eventPublisher.publishEvent(gradePlacedTopic, event).subscribe()
            
            logger.info("Command {} accepted and event published", command.id)
            CommandResponse(
                commandId = command.id,
                status = "ACCEPTED",
                message = "Grade placement command accepted"
            )
        } catch (e: Exception) {
            // Space Core: "Error! ERROR! Something went wrong in spaaaaaace! 
            // Well, not space. In the command handler. But still an error! ERROOOOOOR!"
            // RU: "Ошибка! ОШИБКА! Что-то пошло не так в коооосмосе!
            // Ну, не в космосе. В обработчике команд. Но всё равно ошибка! ОШИИИБКА!"
            // UA: "Помилка! ПОМИЛКА! Щось пішло не так в коооосмосі!
            // Ну, не в космосі. В обробнику команд. Але все одно помилка! ПОМИИИЛКА!"
            logger.error("Error processing command {}", command.id, e)
            commandAuditService.logCommand(command, CommandStatus.FAILED, e.message)
            CommandResponse(
                commandId = command.id,
                status = "FAILED",
                message = "Internal error: ${e.message}"
            )
        }
    }
}

/**
 * Announcer: "Command Response data class. Contains command ID, status, and message. 
 * Simple. Efficient. Effective."
 * RU: "Класс данных ответа команды. Содержит ID команды, статус и сообщение.
 * Просто. Эффективно. Результативно."
 * UA: "Клас даних відповіді команди. Містить ID команди, статус і повідомлення.
 * Просто. Ефективно. Результативно."
 */
data class CommandResponse(
    val commandId: java.util.UUID,
    val status: String,
    val message: String
)
