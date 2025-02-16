package com.poinciana.loganalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poinciana.loganalyzer.entity.LogPattern;
import com.poinciana.loganalyzer.model.LogEntryDTO;
import com.poinciana.loganalyzer.repository.LogPatternRepository;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import io.krakens.grok.api.Match;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LogParserService {

    private static final Logger logger = LoggerFactory.getLogger(LogParserService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    private Grok grok;
    private final ObjectMapper objectMapper;
    private final LogPatternRepository logPatternRepository;

    public LogEntryDTO parseLog(String rawLog, Long patternId) {
        LogEntryDTO logEntryDTO = LogEntryDTO.builder().rawLog(rawLog).build();

        try {
            JsonNode jsonNode = objectMapper.readTree(rawLog);
            logEntryDTO.setTimestamp(LocalDateTime.parse(jsonNode.get("timestamp").asText(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
            logEntryDTO.setLevel(jsonNode.get("level").asText());
            logEntryDTO.setServiceName(jsonNode.get("serviceName").asText());
            logEntryDTO.setMessage(jsonNode.get("message").asText());

            if (jsonNode.has("exception")) {
                logEntryDTO.setException(jsonNode.get("exception").asText());
            }

            // Extract additional metadata dynamically
            jsonNode.fields().forEachRemaining(entry ->
                    logEntryDTO.getMetadata().put(entry.getKey(), entry.getValue().asText()));

            return logEntryDTO;
        } catch (Exception ignored) {
            logger.error("parseLog(): Not a proper json format.");
        }

        // Fetch user/admin-defined log pattern
        Optional<LogPattern> logPatternOpt = patternId != null ?
                logPatternRepository.findById(patternId) : logPatternRepository.findByIsDefaultTrue();

        if (logPatternOpt.isPresent()) {
            Pattern logPattern = Pattern.compile(logPatternOpt.get().getPattern());
            Matcher matcher = logPattern.matcher(rawLog);

            if (matcher.matches()) {
                logEntryDTO.setTimestamp(LocalDateTime.parse(matcher.group(1), TIMESTAMP_FORMATTER));
                logEntryDTO.setLevel(matcher.group(3));
                logEntryDTO.setServiceName(matcher.group(4));
                logEntryDTO.setMessage(matcher.group(5));

                // Extract additional regex groups as metadata
                logger.info("matcher group count: {}", matcher.groupCount());
                for (int i = 0; i <= matcher.groupCount(); i++) {
//                    logEntryDTO.getMetadata().put("field_" + i, matcher.group(i));
                    logger.info("matcher group details: {}", matcher.group(i));
                }
            }
        }

        return logEntryDTO;
    }

    private void initializeGrok(Long patternId) {
        GrokCompiler grokCompiler = GrokCompiler.newInstance();
        grokCompiler.registerDefaultPatterns(); // Register Grok's built-in patterns

        // Fetch user/admin-defined log pattern
        Optional<LogPattern> logPatternOpt = patternId != null ?
                logPatternRepository.findById(patternId) : logPatternRepository.findByIsDefaultTrue();
        // Define log pattern (supports timestamp, level, service, thread, IP, and message)
        String pattern = "%{TIMESTAMP_ISO8601:timestamp} \\[%{DATA:thread}\\] %{LOGLEVEL:level}\\s+(%{DATA:serviceName}) - %{GREEDYDATA:message}";

        this.grok = grokCompiler.compile(pattern);
    }

    public LogEntryDTO grokLogParser(String rawLog, Long patternId) {
        initializeGrok(patternId);
        LogEntryDTO logEntryDTO = LogEntryDTO.builder().rawLog(rawLog).metadata(new HashMap<>()).build();

        try {
            // First, check if it's a JSON log
            JsonNode jsonNode = objectMapper.readTree(rawLog);
            logEntryDTO.setTimestamp(LocalDateTime.parse(jsonNode.get("timestamp").asText(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
            logEntryDTO.setLevel(jsonNode.get("level").asText());
            logEntryDTO.setServiceName(jsonNode.get("serviceName").asText());
            logEntryDTO.setMessage(jsonNode.get("message").asText());

            if (jsonNode.has("exception")) {
                logEntryDTO.setException(jsonNode.get("exception").asText());
            }

            // Extract additional metadata
            jsonNode.fields().forEachRemaining(entry ->
                    logEntryDTO.getMetadata().put(entry.getKey(), entry.getValue().asText()));

            return logEntryDTO;
        } catch (Exception e) {
            logger.warn("parseLog(): Not a JSON log, trying Grok...");
        }

        // Try Grok pattern for plain text logs
        Match match = grok.match(rawLog);
        Map<String, Object> capture = match.capture();

        if (capture.isEmpty()) {
            logger.warn("Failed to parse log: {}", rawLog);
            return logEntryDTO;
        }

        // Update the existing logEntryDTO object instead of creating a new one
        logEntryDTO.setTimestamp(LocalDateTime.parse((String) capture.get("timestamp"), TIMESTAMP_FORMATTER));
        logEntryDTO.setLevel((String) capture.get("level"));
        logEntryDTO.setServiceName((String) capture.get("serviceName"));
        logEntryDTO.setMessage((String) capture.get("message"));
        // Store thread, IP, and other metadata dynamically
        capture.forEach((key, value) -> {
            if (!key.equals("timestamp") && !key.equals("level") && !key.equals("serviceName") && !key.equals("message")) {
                logEntryDTO.getMetadata().put(key, extractSingleValue(value));
            }
        });
        return logEntryDTO;
    }

    private String extractSingleValue(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.isEmpty() ? null : list.get(0).toString();
        }
        return value != null ? value.toString() : null;
    }

}