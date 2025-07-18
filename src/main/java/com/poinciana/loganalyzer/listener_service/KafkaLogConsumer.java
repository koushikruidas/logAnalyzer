package com.poinciana.loganalyzer.listener_service;

import com.poinciana.loganalyzer.entity.LogEntryDocument;
import com.poinciana.loganalyzer.model.LogEntryDTO;
import com.poinciana.loganalyzer.service.LogParserService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class KafkaLogConsumer {
    private final LogParserService logParserService;
    private final BlockingQueue<LogEntryDTO> logQueue;
    private final ScheduledExecutorService bulkProcessor;
    private final ModelMapper mapper;
    private final ElasticsearchTemplate elasticsearchTemplate;

    // Buffer to store the current log message being accumulated
    private final AtomicReference<StringBuilder> logBuffer = new AtomicReference<>(new StringBuilder());


    public KafkaLogConsumer(LogParserService logParserService, ModelMapper mapper, ElasticsearchTemplate elasticsearchTemplate) {
        this.logParserService = logParserService;
        this.mapper = mapper;
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.logQueue = new LinkedBlockingQueue<>(100_000); // High-capacity queue
        this.bulkProcessor = Executors.newScheduledThreadPool(1);
    }

    @PostConstruct
    public void init() {
        // Schedule batch processing to Elasticsearch
        bulkProcessor.scheduleAtFixedRate(this::flushLogsToElasticsearch, 500, 500, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        bulkProcessor.shutdown();
        try {
            if (!bulkProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                bulkProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            bulkProcessor.shutdownNow();
        }
    }

    @KafkaListener(
            topics = "${spring.kafka.consumer.topic}", //Listens to all topics from an organization
            groupId = "${spring.kafka.consumer.groupId}",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "${spring.kafka.consumer.concurrency}"
    )
    public void consumeLogs(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        String topic = "";
        for (ConsumerRecord<String, String> record : records) {
            topic = record.topic();
            String message = record.value();
            processLogMessage(message, topic); // index name and topic name will be same
        }

        // After processing all the records in the batch, if there's an accumulated log entry, process it
        if (!logBuffer.get().isEmpty()) {
            parseAndQueueLog(logBuffer.get().toString(),topic); // index name and topic name will be same
            logBuffer.set(new StringBuilder());  // Assigns a new empty StringBuilder
        }

        if (logQueue.size() > 90_000) {
            log.warn("Log queue near capacity, slowing down Kafka consumption.");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }

        // After processing all records, acknowledge them
        acknowledgment.acknowledge();
    }

    // Process each log message line
    private void processLogMessage(String message, String indexName) {
        // Check if the message starts with a timestamp (this helps to identify a new log entry)
        if (isNewLogEntry(message)) {
            // If we are already buffering a previous log, process it first (flush it)
            if (!logBuffer.get().isEmpty()) {
                parseAndQueueLog(logBuffer.get().toString(), indexName);
                logBuffer.get().setLength(0);  // it will update from beginning removing all old values.
            }
            // Reset the buffer for the new log entry
            logBuffer.set(new StringBuilder(message).append("\n"));
        } else {
            // If it's a continuation, append to the current log
            logBuffer.get().append(message).append("\n");
        }
    }

    // Check if the message starts with a timestamp (indicating it's a new log entry)
    private boolean isNewLogEntry(String message) {
        return message.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*");
    }

    private void parseAndQueueLog(String rawLog, String indexName) {
        try {
            LogEntryDTO logEntryDTO = logParserService.grokLogParser(rawLog, null);
            logEntryDTO.setIndexName(indexName);
            if (!logQueue.offer(logEntryDTO, 50, TimeUnit.MILLISECONDS)) {
                log.warn("Queue full, dropping log entry");
            }
        } catch (Exception e) {
            log.error("Failed to process log: {}", rawLog, e);
        }
    }

    private void flushLogsToElasticsearch() {
        if (logQueue.isEmpty()) return;

        List<LogEntryDTO> batch = new ArrayList<>();
        Map<String, List<IndexQuery>> indexNameToQueries = new HashMap<>();
        logQueue.drainTo(batch, 1000); // Bulk processing 1000 at a time

        if (!batch.isEmpty()) {
            for (LogEntryDTO dto : batch) {
                LogEntryDocument doc = mapper.map(dto, LogEntryDocument.class);

                IndexQuery indexQuery = new IndexQueryBuilder()
                        .withObject(doc)
                        .build();
                indexNameToQueries.computeIfAbsent(dto.getIndexName(), k -> new ArrayList<>()).add(indexQuery);
            }
            for (Map.Entry<String, List<IndexQuery>> entry : indexNameToQueries.entrySet()) {
                String indexName = entry.getKey();
                List<IndexQuery> queries = entry.getValue();
                if (!queries.isEmpty()) {
                    try {
                        elasticsearchTemplate.bulkIndex(queries, IndexCoordinates.of(indexName));
                        log.info("Saved {} logs to index '{}' using bulk API", queries.size(), indexName);
                    } catch (Exception e) {
                        log.error("Error during bulk indexing to index '{}'", indexName, e);
                        // Handle specific index-related errors if needed
                    } finally {
                        // Clear the queries for this index after processing (success or failure)
                        queries.clear(); // Clear the list of queries
                    }
                }
            }
        }
    }

    private boolean isEndOfLogEntry(String nextLine) {
        return (nextLine == null || nextLine.trim().isEmpty()) || nextLine.trim().matches("^\\d{4}-\\d{2}-\\d{2}.*");
    }
}
