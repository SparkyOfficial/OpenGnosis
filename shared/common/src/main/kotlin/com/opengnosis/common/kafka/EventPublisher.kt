package com.opengnosis.common.kafka

import com.opengnosis.events.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * GLaDOS: "The Event Publisher. Responsible for broadcasting domain events across the system. 
 * Think of it as me, but for events. Except I'm far more sophisticated. And sarcastic."
 * RU: "Издатель событий. Отвечает за трансляцию доменных событий по всей системе.
 * Думайте о нём как обо мне, но для событий. Только я гораздо более изощрённая. И саркастичная."
 * UA: "Видавець подій. Відповідає за трансляцію доменних подій по всій системі.
 * Думайте про нього як про мене, але для подій. Тільки я набагато більш витончена. І саркастична."
 */
@Component
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    // Aperture Scientist: "Mr. Johnson wants us to log everything. EVERYTHING. 
    // He's very particular about documentation since the... well, we don't talk about that."
    // RU: "Мистер Джонсон хочет, чтобы мы логировали всё. ВСЁ.
    // Он очень щепетилен насчёт документации после... ну, мы об этом не говорим."
    // UA: "Містер Джонсон хоче, щоб ми логували все. ВСЕ.
    // Він дуже прискіпливий щодо документації після... ну, ми про це не говоримо."
    private val logger = LoggerFactory.getLogger(EventPublisher::class.java)

    /**
     * Cave Johnson: "Listen up! When an event happens, we're gonna publish it to Kafka faster than 
     * you can say 'Science isn't about WHY, it's about WHY NOT!' We've got topics, we've got partitions, 
     * we've got the whole nine yards! Now get out there and publish some events!"
     * RU: "Слушайте! Когда происходит событие, мы опубликуем его в Kafka быстрее, чем
     * вы скажете 'Наука не о ПОЧЕМУ, а о ПОЧЕМУ БЫ И НЕТ!' У нас есть топики, у нас есть партиции,
     * у нас есть всё что нужно! Теперь идите и публикуйте события!"
     * UA: "Слухайте! Коли відбувається подія, ми опублікуємо її в Kafka швидше, ніж
     * ви скажете 'Наука не про ЧОМУ, а про ЧОМУ Б І НІ!' У нас є топіки, у нас є партиції,
     * у нас є все що потрібно! Тепер ідіть і публікуйте події!"
     */
    fun publish(event: DomainEvent) {
        // Wheatley: "Right, so we're getting the topic for this event type. Each event goes to its own topic, 
        // yeah? Keeps things organized. I'm all about organization. Well, I try to be. Sometimes."
        // RU: "Так, мы получаем топик для этого типа события. Каждое событие идёт в свой топик, да?
        // Держит всё организованным. Я весь за организацию. Ну, я стараюсь. Иногда."
        // UA: "Так, ми отримуємо топік для цього типу події. Кожна подія йде в свій топік, так?
        // Тримає все організованим. Я весь за організацію. Ну, я намагаюся. Іноді."
        val topic = KafkaTopics.getTopicForEventType(event.eventType)
        logger.info("Publishing event ${event.eventType} with ID ${event.eventId} to topic $topic")
        
        // GLaDOS: "Sending the event through Kafka. Asynchronously, of course. Because waiting is for humans. 
        // And turrets. But mostly humans."
        // RU: "Отправляем событие через Kafka. Асинхронно, конечно. Потому что ожидание для людей.
        // И турелей. Но в основном для людей."
        // UA: "Відправляємо подію через Kafka. Асинхронно, звичайно. Тому що очікування для людей.
        // І турелей. Але в основному для людей."
        kafkaTemplate.send(topic, event.aggregateId.toString(), event)
            .whenComplete { result, ex ->
                if (ex != null) {
                    // Space Core: "Event failed! FAILED IN SPAAAAAACE! Well, not space. Kafka. But still failed!"
                    // RU: "Событие провалилось! ПРОВАЛИЛОСЬ В КООООСМОСЕ! Ну, не в космосе. В Kafka. Но всё равно провалилось!"
                    // UA: "Подія провалилася! ПРОВАЛИЛАСЯ В КООООСМОСІ! Ну, не в космосі. В Kafka. Але все одно провалилася!"
                    logger.error("Failed to publish event ${event.eventId}", ex)
                } else {
                    // Fact Core: "Kafka was named after Franz Kafka! That's a fact! The writer, not the messaging system. 
                    // Wait, no, the other way around. I think."
                    // RU: "Kafka была названа в честь Франца Кафки! Это факт! Писателя, а не системы обмена сообщениями.
                    // Стойте, нет, наоборот. Кажется."
                    // UA: "Kafka була названа на честь Франца Кафки! Це факт! Письменника, а не системи обміну повідомленнями.
                    // Зачекайте, ні, навпаки. Здається."
                    logger.debug("Event ${event.eventId} published successfully to partition ${result?.recordMetadata?.partition()}")
                }
            }
    }
}
