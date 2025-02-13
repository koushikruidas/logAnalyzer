package com.poinciana.loganalyzer.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.poinciana.loganalyzer.entity.LogEntry;
import com.poinciana.loganalyzer.entity.LogEntryDocument;
import com.poinciana.loganalyzer.repository.LogEntryElasticsearchRepository;
import com.poinciana.loganalyzer.repository.LogEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class LogService {
    @Autowired(required = false)
    private LogEntryElasticsearchRepository logEntryElasticsearchRepository;

    private final ElasticsearchOperations elasticsearchOperations;
    private final LogEntryRepository logEntryRepository;
    private final LogParserService logParserService;
    private final ElasticsearchClient elasticsearchClient;

    @Transactional
    public LogEntry ingestLog(String rawLog, Long patternId) {
        // Parse the log
        LogEntry logEntry = logParserService.parseLog(rawLog, patternId);
        logEntry = logEntryRepository.save(logEntry);

        // Save log in Elasticsearch
        LogEntryDocument logEntryDocument = LogEntryDocument.builder()
                .id(logEntry.getId().toString())
                .timestamp(logEntry.getTimestamp())
                .level(logEntry.getLevel())
                .serviceName(logEntry.getServiceName())
                .message(logEntry.getMessage())
                .exception(logEntry.getException())
                .rawLog(logEntry.getRawLog())
                .build();
        logEntryElasticsearchRepository.save(logEntryDocument);

        return logEntry;
    }

    public List<LogEntry> getAllLogs() {
        return logEntryRepository.findAll();
    }

    public List<LogEntry> getFilteredLogs(String level, String serviceName, LocalDateTime startDate, LocalDateTime endDate) {
        return logEntryRepository.findFilteredLogs(level, serviceName, startDate, endDate);
    }

    public SearchHits<LogEntryDocument> searchLogs(String level, String serviceName, String keyword,
                                             LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        Criteria criteria = new Criteria();

        if (level != null) {
            criteria.and(Criteria.where("level").is(level));
        }
        if (serviceName != null) {
            criteria.and(Criteria.where("serviceName").is(serviceName));
        }
        if (keyword != null) {
            criteria.and(Criteria.where("message").contains(keyword));
        }
        if (startDate != null && endDate != null) {
            criteria.and(Criteria.where("timestamp").between(startDate, endDate));
        }

        CriteriaQuery query = new CriteriaQuery(criteria).setPageable(PageRequest.of(page, size));


        return elasticsearchOperations.search(query, LogEntryDocument.class);
    }

    public Map<String, Long> getLogCountByLevel() throws IOException {
        return executeAggregationQuery("log_levels", "level.keyword");
    }

    public Map<String, Long> getLogCountByService() throws IOException {
        return executeAggregationQuery("service_count", "serviceName.keyword");
    }

    public Map<String, Long> getLogCountByDate() throws IOException {
        return executeAggregationQuery("log_by_date", "timestamp");
    }

    private Map<String, Long> executeAggregationQuery(String aggName, String fieldName) throws IOException {
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index("logs") // Use your Elasticsearch index
                .size(0) // No need to fetch documents, just aggregation
                .aggregations(aggName, Aggregation.of(a -> a
                        .terms(t -> t.field(fieldName))
                ))
        );

        SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

        Map<String, Long> result = new HashMap<>();
        var aggregation = response.aggregations().get(aggName).sterms().buckets().array();
        aggregation.forEach(bucket -> result.put(bucket.key().stringValue(), bucket.docCount()));

        return result;
    }

}

