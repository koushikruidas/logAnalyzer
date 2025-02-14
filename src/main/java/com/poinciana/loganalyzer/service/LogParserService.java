package com.poinciana.loganalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poinciana.loganalyzer.entity.LogEntry;
import com.poinciana.loganalyzer.entity.LogPattern;
import com.poinciana.loganalyzer.repository.LogPatternRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LogParserService {

    private final ObjectMapper objectMapper;
    private final LogPatternRepository logPatternRepository;

    public LogEntry parseLog(String rawLog, Long patternId) {
        LogEntry logEntry = LogEntry.builder().rawLog(rawLog).build();

        try {
            JsonNode jsonNode = objectMapper.readTree(rawLog);
            logEntry.setTimestamp(LocalDateTime.parse(jsonNode.get("timestamp").asText(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
            logEntry.setLevel(jsonNode.get("level").asText());
            logEntry.setServiceName(jsonNode.get("serviceName").asText());
            logEntry.setMessage(jsonNode.get("message").asText());

            if (jsonNode.has("exception")) {
                logEntry.setException(jsonNode.get("exception").asText());
            }
            return logEntry;
        } catch (Exception ignored) {
        }

        // Fetch user/admin-defined log pattern
        Optional<LogPattern> logPatternOpt = patternId != null ?
            logPatternRepository.findById(patternId) : logPatternRepository.findByIsDefaultTrue();

        if (logPatternOpt.isPresent()) {
            Pattern logPattern = Pattern.compile(logPatternOpt.get().getPattern());
            Matcher matcher = logPattern.matcher(rawLog);

            if (matcher.matches()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                logEntry.setTimestamp(LocalDateTime.parse(matcher.group(1), formatter));
                logEntry.setLevel(matcher.group(3));
                logEntry.setServiceName(matcher.group(4));
                logEntry.setMessage(matcher.group(5));
            }
        }

        return logEntry;
    }
}