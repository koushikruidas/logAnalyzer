package com.poinciana.loganalyzer.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.poinciana.loganalyzer.entity.LogEntry;
import com.poinciana.loganalyzer.entity.LogEntryDocument;
import com.poinciana.loganalyzer.model.LogEntryDTO;
import com.poinciana.loganalyzer.model.LogSearchResponseDTO;
import com.poinciana.loganalyzer.repository.LogEntryElasticsearchRepository;
import com.poinciana.loganalyzer.repository.LogEntryRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogService {

    private final static Logger logger = LoggerFactory.getLogger(LogService.class);
    @Autowired(required = false)
    private LogEntryElasticsearchRepository logEntryElasticsearchRepository;
    @Value("${log.persistence.enableRelationalDB}")
    private boolean enableRelationalDB;
    @Value("${log.ingest.batch-size}")
    private int batchSize;
    @Value("${log.ingest.enable-host-lookup:false}") // Configurable: Disable host lookup if needed
    private boolean enableHostLookup;

    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);


    private final ElasticsearchOperations elasticsearchOperations;
    private final LogEntryRepository logEntryRepository;
    private final LogParserService logParserService;
    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ModelMapper modelMapper;

    @Transactional
    public LogEntryDTO ingestLog(String rawLog, Long patternId) {
        // Parse the log
        LogEntryDTO logEntryDTO = logParserService.grokLogParser(rawLog,patternId);

        // Capture Host Details
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            logEntryDTO.setHostName(inetAddress.getHostName());
            logEntryDTO.setHostIp(inetAddress.getHostAddress());
        } catch (UnknownHostException e) {
            logger.warn("Failed to retrieve host details",e);
        }

        LogEntry logEntry = modelMapper.map(logEntryDTO, LogEntry.class);
        LogEntryDocument entryDocument = modelMapper.map(logEntryDTO, LogEntryDocument.class);

        if (enableRelationalDB) {
            logEntry.setId(logEntryDTO.getId() != null ? Long.parseLong(logEntryDTO.getId()) : null);
            logEntryRepository.save(logEntry);
        }
        // Save log in Elasticsearch
        LogEntryDocument save = logEntryElasticsearchRepository.save(entryDocument);
        logEntryDTO.setId(save.getId());
        return logEntryDTO;
    }

    public List<LogEntry> getAllLogs() {
        return logEntryRepository.findAll();
    }

    public List<LogEntry> getFilteredLogs(String level, String serviceName, LocalDateTime startDate, LocalDateTime endDate) {
        return logEntryRepository.findFilteredLogs(level, serviceName, startDate, endDate);
    }

    public LogSearchResponseDTO searchLogs(String level, String serviceName, String keyword,
                                           LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        Criteria criteria = new Criteria();

        if (level != null) {
            criteria.and(Criteria.where("level").is(level));
        }
        if (serviceName != null) {
            criteria.and(Criteria.where("serviceName").is(serviceName));
        }
        if (keyword != null) {
            criteria.and(Criteria.where("message").is(keyword));
        }
        if (startDate != null && endDate != null) {
            criteria.and(Criteria.where("timestamp").between(startDate, endDate));
        }

        CriteriaQuery query = new CriteriaQuery(criteria).setPageable(PageRequest.of(page, size));
        SearchHits<LogEntryDocument> searchHits = elasticsearchOperations.search(query, LogEntryDocument.class);

        List<LogEntryDTO> logEntries = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(log -> modelMapper.map(log, LogEntryDTO.class))
                .collect(Collectors.toList());

        return new LogSearchResponseDTO(logEntries, searchHits.getTotalHits());
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
    @Transactional
    public List<LogEntryDTO> ingestLogFile(MultipartFile file, Long patternId, String indexName) {
        List<CompletableFuture<LogEntryDTO>> futures = new ArrayList<>();
        List<LogEntryDocument> batchDocuments = new ArrayList<>(batchSize);

        ensureIndexExists(indexName); // Ensure index is created before processing logs

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            StringBuilder logBuilder = new StringBuilder();
            String currentLine, nextLine = reader.readLine();

            while ((currentLine = nextLine) != null) {
                nextLine = reader.readLine();
                if (currentLine.trim().isEmpty()) continue; // Skip empty lines

                logBuilder.append(currentLine).append("\n");

                if (isEndOfLogEntry(nextLine)) {
                    String rawLog = logBuilder.toString().trim();
                    futures.add(parseAndStoreLog(rawLog, patternId, batchDocuments));

                    if (batchDocuments.size() >= adaptiveBatchSize()) {
                        saveBulk(batchDocuments, indexName);
                        batchDocuments.clear();
                    }

                    logBuilder.setLength(0); // Clear buffer for next log
                }
            }

            // Process the last log entry
            if (!logBuilder.isEmpty()) {
                String rawLog = logBuilder.toString().trim();
                futures.add(parseAndStoreLog(rawLog, patternId, batchDocuments));
            }

            // Wait for all parsing tasks to finish
            List<LogEntryDTO> logEntries = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Save any remaining batch
            if (!batchDocuments.isEmpty()) {
                saveBulk(batchDocuments, indexName);
            }

            return logEntries;

        } catch (IOException e) {
            throw new RuntimeException("Error reading log file", e);
        }
    }

    private CompletableFuture<LogEntryDTO> parseAndStoreLog(String rawLog, Long patternId, List<LogEntryDocument> batchDocuments) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LogEntryDTO logEntryDTO = logParserService.grokLogParser(rawLog, patternId);

                if (enableHostLookup) {
                    setHostDetails(logEntryDTO);
                }

                batchDocuments.add(modelMapper.map(logEntryDTO, LogEntryDocument.class));
                return logEntryDTO;
            } catch (Exception e) {
                logger.error("Failed to process log entry: " + rawLog, e);
                return null;
            }
        }, executor);
    }

    private void saveBulk(List<LogEntryDocument> batchDocuments, String indexName) {
        try {
            IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
            elasticsearchTemplate.save(batchDocuments, indexCoordinates);
            logger.info("Saved {} logs to Elasticsearch index: {}", batchDocuments.size(), indexName);
        } catch (Exception e) {
            logger.error("Failed to save batch to Elasticsearch", e);
            // Optionally, log the failed batch for later retry
        }
    }

    private void ensureIndexExists(String indexName) {
        if (!elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName)).exists()) {
            elasticsearchTemplate.indexOps(IndexCoordinates.of(indexName)).create();
            logger.info("Created index: {}", indexName);
        }
    }

    private void setHostDetails(LogEntryDTO logEntryDTO) {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            logEntryDTO.setHostName(inetAddress.getHostName());
            logEntryDTO.setHostIp(inetAddress.getHostAddress());
        } catch (UnknownHostException e) {
            logger.warn("Failed to retrieve host details", e);
        }
    }

    private int adaptiveBatchSize() {
        long freeMemory = Runtime.getRuntime().freeMemory();
        return freeMemory < 100_000_000 ? batchSize / 2 : batchSize; // Reduce batch size if memory is low
    }

    private boolean isEndOfLogEntry(String nextLine) {
        return nextLine != null && nextLine.trim().matches("^\\d{4}-\\d{2}-\\d{2}.*");
    }
}

