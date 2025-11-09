package com.opengnosis.common.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ElasticsearchConfig {

    @Value("\${elasticsearch.host:elasticsearch-service}")
    private lateinit var elasticsearchHost: String

    @Value("\${elasticsearch.port:9200}")
    private var elasticsearchPort: Int = 9200

    @Bean
    fun elasticsearchClient(): ElasticsearchClient {
        val restClient = RestClient.builder(
            HttpHost(elasticsearchHost, elasticsearchPort, "http")
        ).build()

        val objectMapper = ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        val transport = RestClientTransport(
            restClient,
            JacksonJsonpMapper(objectMapper)
        )

        return ElasticsearchClient(transport)
    }
}
