package com.opengnosis.journal.service

import com.opengnosis.events.BaseDomainEvent
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ReactiveEventPublisher(
    private val reactiveKafkaProducerTemplate: ReactiveKafkaProducerTemplate<String, BaseDomainEvent>
) {
    private val logger = LoggerFactory.getLogger(ReactiveEventPublisher::class.java)
    
    fun publishEvent(topic: String, event: BaseDomainEvent): Mono<Void> {
        val record = ProducerRecord(topic, event.aggregateId.toString(), event)
        
        return reactiveKafkaProducerTemplate
            .send(record)
            .doOnSuccess { result ->
                logger.info(
                    "Successfully published event {} to topic {} at offset {}",
                    event.eventType,
                    topic,
                    result.recordMetadata().offset()
                )
            }
            .doOnError { error ->
                logger.error("Failed to publish event {} to topic {}", event.eventType, topic, error)
            }
            .then()
    }
}
