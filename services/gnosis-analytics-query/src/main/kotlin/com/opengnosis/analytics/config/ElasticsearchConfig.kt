package com.opengnosis.analytics.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Configuration
@EnableElasticsearchRepositories(basePackages = ["com.opengnosis.analytics.repository"])
class ElasticsearchConfig(
    @Value("\${spring.elasticsearch.uris}") private val elasticsearchUris: String,
    @Value("\${spring.elasticsearch.username}") private val username: String,
    @Value("\${spring.elasticsearch.password}") private val password: String
) : ElasticsearchConfiguration() {
    
    override fun clientConfiguration(): ClientConfiguration {
        return ClientConfiguration.builder()
            .connectedTo(elasticsearchUris.removePrefix("http://").removePrefix("https://"))
            .withBasicAuth(username, password)
            .withConnectTimeout(java.time.Duration.ofSeconds(5))
            .withSocketTimeout(java.time.Duration.ofSeconds(60))
            .build()
    }
}
