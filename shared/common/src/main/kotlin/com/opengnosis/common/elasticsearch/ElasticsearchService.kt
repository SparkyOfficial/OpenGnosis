package com.opengnosis.common.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch.core.*
import co.elastic.clients.elasticsearch.core.search.Hit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for interacting with Elasticsearch.
 * Provides common indexing and search operations.
 */
@Service
class ElasticsearchService(
    private val elasticsearchClient: ElasticsearchClient
) {
    private val logger = LoggerFactory.getLogger(ElasticsearchService::class.java)

    /**
     * Index a document.
     */
    fun <T> index(indexName: String, id: String, document: T): Boolean {
        return try {
            val response = elasticsearchClient.index { builder ->
                builder
                    .index(indexName)
                    .id(id)
                    .document(document)
            }
            logger.debug("Indexed document with ID $id in index $indexName")
            response.result().name == "Created" || response.result().name == "Updated"
        } catch (ex: Exception) {
            logger.error("Failed to index document with ID $id in index $indexName", ex)
            false
        }
    }

    /**
     * Bulk index documents.
     */
    fun <T> bulkIndex(indexName: String, documents: Map<String, T>): Boolean {
        return try {
            val bulkRequest = BulkRequest.Builder()
            
            documents.forEach { (id, document) ->
                bulkRequest.operations { op ->
                    op.index { idx ->
                        idx
                            .index(indexName)
                            .id(id)
                            .document(document)
                    }
                }
            }
            
            val response = elasticsearchClient.bulk(bulkRequest.build())
            logger.debug("Bulk indexed ${documents.size} documents in index $indexName")
            !response.errors()
        } catch (ex: Exception) {
            logger.error("Failed to bulk index documents in index $indexName", ex)
            false
        }
    }

    /**
     * Get a document by ID.
     */
    fun <T> get(indexName: String, id: String, clazz: Class<T>): T? {
        return try {
            val response = elasticsearchClient.get({ builder ->
                builder
                    .index(indexName)
                    .id(id)
            }, clazz)
            
            if (response.found()) {
                logger.debug("Retrieved document with ID $id from index $indexName")
                response.source()
            } else {
                logger.debug("Document with ID $id not found in index $indexName")
                null
            }
        } catch (ex: Exception) {
            logger.error("Failed to get document with ID $id from index $indexName", ex)
            null
        }
    }

    /**
     * Search documents.
     */
    fun <T> search(indexName: String, query: Query, clazz: Class<T>, size: Int = 10): List<T> {
        return try {
            val response = elasticsearchClient.search({ builder ->
                builder
                    .index(indexName)
                    .query(query)
                    .size(size)
            }, clazz)
            
            val results = response.hits().hits().mapNotNull { it.source() }
            logger.debug("Search returned ${results.size} results from index $indexName")
            results
        } catch (ex: Exception) {
            logger.error("Failed to search in index $indexName", ex)
            emptyList()
        }
    }

    /**
     * Delete a document by ID.
     */
    fun delete(indexName: String, id: String): Boolean {
        return try {
            val response = elasticsearchClient.delete { builder ->
                builder
                    .index(indexName)
                    .id(id)
            }
            logger.debug("Deleted document with ID $id from index $indexName")
            response.result().name == "Deleted"
        } catch (ex: Exception) {
            logger.error("Failed to delete document with ID $id from index $indexName", ex)
            false
        }
    }

    /**
     * Update a document.
     */
    fun <T> update(indexName: String, id: String, document: T): Boolean {
        return try {
            val response = elasticsearchClient.update<T, T>({ builder ->
                builder
                    .index(indexName)
                    .id(id)
                    .doc(document)
            }, Any::class.java)
            logger.debug("Updated document with ID $id in index $indexName")
            response.result().name == "Updated"
        } catch (ex: Exception) {
            logger.error("Failed to update document with ID $id in index $indexName", ex)
            false
        }
    }

    /**
     * Check if an index exists.
     */
    fun indexExists(indexName: String): Boolean {
        return try {
            elasticsearchClient.indices().exists { builder ->
                builder.index(indexName)
            }.value()
        } catch (ex: Exception) {
            logger.error("Failed to check if index $indexName exists", ex)
            false
        }
    }
}
