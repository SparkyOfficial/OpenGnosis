package com.opengnosis.journal.config

import com.opengnosis.events.BaseDomainEvent
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.kafka.support.serializer.JsonSerializer
import reactor.kafka.sender.SenderOptions

@Configuration
class ReactiveKafkaConfig {
    
    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String
    
    @Bean
    fun reactiveKafkaProducerTemplate(): ReactiveKafkaProducerTemplate<String, BaseDomainEvent> {
        val props = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 1,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            JsonSerializer.ADD_TYPE_INFO_HEADERS to false
        )
        
        val senderOptions = SenderOptions.create<String, BaseDomainEvent>(props)
        return ReactiveKafkaProducerTemplate(senderOptions)
    }
}
