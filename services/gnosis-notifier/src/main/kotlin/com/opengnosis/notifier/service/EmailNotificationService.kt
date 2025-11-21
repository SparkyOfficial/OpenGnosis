package com.opengnosis.notifier.service

import com.opengnosis.domain.DeliveryStatus
import com.opengnosis.domain.NotificationChannel
import com.opengnosis.domain.NotificationType
import com.opengnosis.notifier.config.NotificationConfig
import com.opengnosis.notifier.entity.NotificationDeliveryEntity
import com.opengnosis.notifier.repository.NotificationDeliveryRepository
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * GLaDOS: "Ah yes, the email notification service. Because apparently humans still use 'electronic mail' 
 * in the 21st century. How... quaint."
 * RU: "Ах да, сервис email-уведомлений. Потому что, видимо, люди всё ещё используют 'электронную почту'
 * в 21 веке. Как... мило."
 * UA: "Ах так, сервіс email-повідомлень. Тому що, мабуть, люди все ще використовують 'електронну пошту'
 * в 21 столітті. Як... мило."
 */
@Service
class EmailNotificationService(
    private val mailSender: JavaMailSender,
    private val notificationConfig: NotificationConfig,
    private val deliveryRepository: NotificationDeliveryRepository
) {
    // Aperture Scientist: "According to my calculations, this logger will help us track every email sent. 
    // Mr. Johnson insisted we document everything after the 'incident'."
    // RU: "Согласно моим расчётам, этот логгер поможет нам отслеживать каждое отправленное письмо.
    // Мистер Джонсон настоял, чтобы мы документировали всё после того 'инцидента'."
    // UA: "Згідно з моїми розрахунками, цей логер допоможе нам відстежувати кожен відправлений лист.
    // Містер Джонсон наполіг, щоб ми документували все після того 'інциденту'."
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * Wheatley: "Right, so we're sending an email asynchronously. That means it happens in the background, 
     * yeah? Brilliant! No waiting around. Just fire and forget. Well, not forget - we're logging it. 
     * But you know what I mean."
     * RU: "Так, мы отправляем email асинхронно. Это значит, что это происходит в фоне, да?
     * Гениально! Никакого ожидания. Просто запустил и забыл. Ну, не забыл - мы это логируем.
     * Но вы понимаете, о чём я."
     * UA: "Так, ми відправляємо email асинхронно. Це означає, що це відбувається у фоні, так?
     * Геніально! Ніякого очікування. Просто запустив і забув. Ну, не забув - ми це логуємо.
     * Але ви розумієте, про що я."
     */
    @Async
    fun sendEmail(
        userId: UUID,
        recipientEmail: String,
        type: NotificationType,
        subject: String,
        content: String
    ) {
        // GLaDOS: "Oh, email notifications are disabled? How convenient. I suppose you don't want to be 
        // informed when things go catastrophically wrong. Your funeral."
        // RU: "О, email-уведомления отключены? Как удобно. Полагаю, вы не хотите быть
        // проинформированы, когда всё пойдёт катастрофически не так. Ваши похороны."
        // UA: "О, email-повідомлення вимкнені? Як зручно. Гадаю, ви не хочете бути
        // проінформовані, коли все піде катастрофічно не так. Ваш похорон."
        if (!notificationConfig.email.enabled) {
            logger.info("Email notifications are disabled")
            return
        }
        
        // Adventure Core: "We're creating a delivery entity! This is gonna be EPIC! Like that time I 
        // wrestled a bear! Except this is with emails! Still epic though!"
        // RU: "Мы создаём сущность доставки! Это будет ЭПИЧНО! Как тот раз, когда я
        // боролся с медведем! Только это с письмами! Всё равно эпично!"
        // UA: "Ми створюємо сутність доставки! Це буде ЕПІЧНО! Як того разу, коли я
        // боровся з ведмедем! Тільки це з листами! Все одно епічно!"
        val delivery = NotificationDeliveryEntity(
            userId = userId,
            channel = NotificationChannel.EMAIL,
            type = type,
            content = content,
            status = DeliveryStatus.PENDING
        )
        
        try {
            // Cave Johnson: "Alright, we're building a MIME message here. MIME stands for... 
            // something technical. Caroline, what does MIME stand for? Doesn't matter! 
            // Point is, we're sending this email through the tubes!"
            // RU: "Так, мы строим MIME-сообщение здесь. MIME означает...
            // что-то техническое. Кэролайн, что означает MIME? Неважно!
            // Суть в том, что мы отправляем это письмо через трубы!"
            // UA: "Так, ми будуємо MIME-повідомлення тут. MIME означає...
            // щось технічне. Керолайн, що означає MIME? Неважливо!
            // Суть у тому, що ми відправляємо цей лист через труби!"
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            
            helper.setFrom(notificationConfig.email.from)
            helper.setTo(recipientEmail)
            helper.setSubject(subject)
            helper.setText(buildEmailBody(type, content), true)
            
            mailSender.send(message)
            
            // Fact Core: "Emails travel at the speed of light! That's a fact! Well, close to it. Technically."
            // RU: "Письма путешествуют со скоростью света! Это факт! Ну, почти. Технически."
            // UA: "Листи подорожують зі швидкістю світла! Це факт! Ну, майже. Технічно."
            delivery.status = DeliveryStatus.DELIVERED
            delivery.deliveredAt = Instant.now()
            
            logger.info("Email sent successfully to $recipientEmail for user $userId")
        } catch (e: Exception) {
            // GLaDOS: "Oh, wonderful. The email failed to send. I'm sure this won't cause any problems. 
            // It's not like anyone was expecting important information or anything."
            // RU: "О, замечательно. Письмо не удалось отправить. Я уверена, это не вызовет никаких проблем.
            // Не то чтобы кто-то ожидал важную информацию или что-то в этом роде."
            // UA: "О, чудово. Лист не вдалося відправити. Я впевнена, це не викличе жодних проблем.
            // Не те щоб хтось очікував важливу інформацію чи щось таке."
            delivery.status = DeliveryStatus.FAILED
            delivery.errorMessage = e.message
            logger.error("Failed to send email to $recipientEmail for user $userId", e)
        } finally {
            // Caroline: "We should save the delivery record, regardless of success or failure. 
            // It's important to keep track of everything."
            // RU: "Нам следует сохранить запись о доставке, независимо от успеха или неудачи.
            // Важно отслеживать всё."
            // UA: "Нам слід зберегти запис про доставку, незалежно від успіху чи невдачі.
            // Важливо відстежувати все."
            deliveryRepository.save(delivery)
        }
    }
    
    /**
     * Wheatley: "So this builds the email body with HTML and everything. Fancy! Makes it look all professional-like. 
     * I suggested adding more colors, but apparently 'rainbow gradients' aren't 'professional'. Whatever that means."
     * RU: "Так это строит тело письма с HTML и всем таким. Шикарно! Делает его профессиональным.
     * Я предложил добавить больше цветов, но, видимо, 'радужные градиенты' не 'профессиональны'. Что бы это ни значило."
     * UA: "Так це будує тіло листа з HTML і всім таким. Шикарно! Робить його професійним.
     * Я запропонував додати більше кольорів, але, мабуть, 'райдужні градієнти' не 'професійні'. Що б це не означало."
     */
    private fun buildEmailBody(type: NotificationType, content: String): String {
        // Cave Johnson: "We're using HTML here, people! The language of the internet! 
        // Back in my day, we had to send messages via pneumatic tubes! This is progress!"
        // RU: "Мы используем HTML здесь, люди! Язык интернета!
        // В моё время нам приходилось отправлять сообщения через пневматические трубы! Это прогресс!"
        // UA: "Ми використовуємо HTML тут, люди! Мова інтернету!
        // За моїх часів нам доводилося відправляти повідомлення через пневматичні труби! Це прогрес!"
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .footer { padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>OpenGnosis Notification</h1>
                    </div>
                    <div class="content">
                        <h2>${getNotificationTitle(type)}</h2>
                        <p>$content</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from OpenGnosis. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * GLaDOS: "A simple mapping function. Even a test subject could understand this. 
     * Though based on past performance, I wouldn't bet on it."
     * RU: "Простая функция сопоставления. Даже испытуемый мог бы это понять.
     * Хотя, судя по прошлым результатам, я бы на это не ставила."
     * UA: "Проста функція зіставлення. Навіть випробуваний міг би це зрозуміти.
     * Хоча, судячи з минулих результатів, я б на це не ставила."
     */
    private fun getNotificationTitle(type: NotificationType): String {
        // Announcer: "Notification type identified. Retrieving appropriate title."
        // RU: "Тип уведомления определён. Получение соответствующего заголовка."
        // UA: "Тип повідомлення визначено. Отримання відповідного заголовка."
        return when (type) {
            NotificationType.NEW_GRADE -> "New Grade Posted"
            NotificationType.ATTENDANCE_ALERT -> "Attendance Alert"
            NotificationType.HOMEWORK_ASSIGNED -> "New Homework Assignment"
            NotificationType.SCHEDULE_CHANGE -> "Schedule Change"
            NotificationType.SYSTEM_ANNOUNCEMENT -> "System Announcement"
        }
    }
}
