package com.poinciana.loganalyzer.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "log.alert")
@Data
public class LogAlertConfig {
    // Elasticsearch Config
    @Value("${elasticsearch.host}")
    private String elasticsearchHost;

    @Value("${elasticsearch.index}")
    private String elasticsearchIndex;

    @Value("${elasticsearch.username}")
    private String elasticsearchUsername;

    @Value("${elasticsearch.password}")
    private String elasticsearchPassword;

    // Log Ingestion & Parsing
    @Value("${log.ingest.batch-size}")
    private int batchSize;

    @Value("${log.ingest.default-pattern}")
    private String defaultLogPattern;

    // Log Storage Optimization
    @Value("${log.storage.retention-days}")
    private int retentionDays;

    // Log Search & Filtering
    @Value("${log.search.page-size}")
    private int pageSize;

    @Value("${log.search.max-results}")
    private int maxResults;

    // Log Aggregation
    @Value("${log.aggregation.enabled}")
    private boolean aggregationEnabled;

    @Value("${log.aggregation.interval}")
    private String aggregationInterval;

    @Value("${log.aggregation.fields}")
    private String aggregationFields;

    // Log Alerting
    @Value("${log.alert.time-window-minutes}")
    private int alertTimeWindowMinutes;

    @Value("${log.alert.error-threshold}")
    private int errorThreshold;

    @Value("${log.alert.webhook-url}")
    private String webhookUrl;

    // Custom Log Parsing
    @Value("${log.parsing.allow-custom-patterns}")
    private boolean allowCustomPatterns;

    @Value("${log.parsing.default-pattern}")
    private String logParsingDefaultPattern;
}