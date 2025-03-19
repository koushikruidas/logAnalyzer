package com.poinciana.loganalyzer.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ElasticsearchIndexInitializer {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @PostConstruct
    public void createIndexIfNotExists() {
        try {
            String indexName = "log_entries";  // Default index

            // Check if index exists
            BooleanResponse exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(indexName)));

            if (!exists.value()) {
                // Create index if not exists
                elasticsearchClient.indices()
                        .create(CreateIndexRequest.of(c -> c.index(indexName)));
                System.out.println("Default Index 'log_entries' created successfully.");
            } else {
                System.out.println("Default Index 'log_entries' already exists.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to check or create Elasticsearch default index", e);
        }
    }
}
