package com.poinciana.loganalyzer.listener_service;

import com.poinciana.loganalyzer.entity.LogEntryDocument;
import com.poinciana.loganalyzer.model.LogEntryDTO;
import com.poinciana.loganalyzer.repository.LogEntryElasticsearchRepository;
import com.poinciana.loganalyzer.service.LogParserService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class KafkaLogConsumer {

    private final LogParserService logParserService;
    private final LogEntryElasticsearchRepository logEntryElasticsearchRepository;
    private final BlockingQueue<LogEntryDocument> logQueue;
    private final ScheduledExecutorService bulkProcessor;
    private final ModelMapper mapper;

    @Value("${spring.kafka.consumer.enableHostLookup:false}")
    private boolean enableHostLookup;

    private String hostName;
    private String hostIp;

    // Buffer to store the current log message being accumulated
    private final AtomicReference<StringBuilder> logBuffer = new AtomicReference<>(new StringBuilder());



    public KafkaLogConsumer(LogParserService logParserService,
                            LogEntryElasticsearchRepository logEntryElasticsearchRepository, ModelMapper mapper) {
        this.logParserService = logParserService;
        this.logEntryElasticsearchRepository = logEntryElasticsearchRepository;
        this.mapper = mapper;
        this.logQueue = new LinkedBlockingQueue<>(100_000); // High-capacity queue
        this.bulkProcessor = Executors.newScheduledThreadPool(1);
    }

    @PostConstruct
    public void init() {
        if (enableHostLookup) {
            try {
                InetAddress inetAddress = InetAddress.getLocalHost();
                this.hostName = inetAddress.getHostName();
                this.hostIp = inetAddress.getHostAddress();
            } catch (UnknownHostException e) {
                log.warn("Failed to retrieve host details", e);
            }
        }

        // Schedule batch processing to Elasticsearch
        bulkProcessor.scheduleAtFixedRate(this::flushLogsToElasticsearch, 500, 500, TimeUnit.MILLISECONDS);
    }

    @KafkaListener(
        topics = "${spring.kafka.consumer.topic.single}",
        groupId = "${spring.kafka.consumer.groupId}",
        containerFactory = "kafkaListenerContainerFactory",
        concurrency = "${spring.kafka.consumer.concurrency}"
    )
    public void consumeLogs(List<String> messages, Acknowledgment acknowledgment) {
        for (String message : messages) {
            processLogMessage(message);
        }

        // After processing all the messages in the batch, if there's an accumulated log entry, process it
        if (!logBuffer.get().isEmpty()) {
            parseAndQueueLog(logBuffer.get().toString());
            logBuffer.set(new StringBuilder());  // Assigns a new empty StringBuilder
        }

        // After processing all messages, acknowledge them
        acknowledgment.acknowledge();
    }

    // Process each log message line
    private void processLogMessage(String message) {
        // Check if the message starts with a timestamp (this helps to identify a new log entry)
        if (isNewLogEntry(message)) {
            // If we are already buffering a previous log, process it first (flush it)
            if (!logBuffer.get().isEmpty()) {
                parseAndQueueLog(logBuffer.get().toString());
                logBuffer.set(new StringBuilder());  // Assigns a new empty StringBuilder
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

    private void parseAndQueueLog(String rawLog) {
        try {
            LogEntryDTO logEntryDTO = logParserService.grokLogParser(rawLog, null);

            if (enableHostLookup) {
                logEntryDTO.setHostName(hostName);
                logEntryDTO.setHostIp(hostIp);
            }

            if (!logQueue.offer(mapper.map(logEntryDTO, LogEntryDocument.class), 50, TimeUnit.MILLISECONDS)) {
                log.warn("Queue full, dropping log entry");
            }
        } catch (Exception e) {
            log.error("Failed to process log: {}", rawLog, e);
        }
    }

    private void flushLogsToElasticsearch() {
        if (logQueue.isEmpty()) return;

        List<LogEntryDocument> batch = new ArrayList<>();
        logQueue.drainTo(batch, 1000); // Bulk processing 1000 at a time

        if (!batch.isEmpty()) {
            logEntryElasticsearchRepository.saveAll(batch);
            log.info("Saved {} logs to Elasticsearch", batch.size());
        }
    }

    private boolean isEndOfLogEntry(String nextLine) {
        return (nextLine == null || nextLine.trim().isEmpty()) || nextLine.trim().matches("^\\d{4}-\\d{2}-\\d{2}.*");
    }
}
