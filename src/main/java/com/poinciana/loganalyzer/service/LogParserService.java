package com.poinciana.loganalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poinciana.loganalyzer.model.LogEntryDTO;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import io.krakens.grok.api.Match;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Robust log parser handling:
 *  - JSON logs from Log4j2 JsonLayout (many possible field names)
 *  - Text logs matched via Grok for PatternLayout
 *  - Multiple timestamp formats (ISO offsets, space formats with/without millis, epoch millis/seconds)
 *
 * Example usage:
 *   LogParserService parser = new LogParserService();
 *   LogEntryDTO dto = parser.grokLogParser(rawLog, null);
 */
@Slf4j
@Service
public class LogParserService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Grok compiled once, reused
    private final Grok grok;

    // Default zone to use when timestamps lack zone info
    private final ZoneId defaultZone = ZoneId.systemDefault();

    // Common formatters
    private static final DateTimeFormatter SPACE_MS_COMMA_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");
    private static final DateTimeFormatter SPACE_MS_DOT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter SPACE_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_WITH_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final DateTimeFormatter ISO_NO_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public LogParserService() {
        this.grok = initializeGrok();
    }

    /**
     * Initialize and compile Grok pattern once.
     * Pattern is permissive: optional thread, captures level, logger, timestamp, message.
     */
    private Grok initializeGrok() {
        GrokCompiler grokCompiler = GrokCompiler.newInstance();
        grokCompiler.registerDefaultPatterns();

        // Pattern supports:
        //  2025-11-01 18:26:50,077 [thread-name] INFO com.example.Service - the message...
        // Thread is optional to handle variants.
        String pattern = "%{TIMESTAMP_ISO8601:timestamp} \\[%{DATA:thread}\\] %{LOGLEVEL:level}\\s+(%{DATA:serviceName}) - %{GREEDYDATA:message}";

        return grokCompiler.compile(pattern);
    }

    /**
     * Main parser method. Returns a LogEntryDTO populated as best-effort.
     *
     * @param rawLog   raw log text consumed from Kafka (may be JSON or plain text)
     * @param patternId optional pattern id (kept for API compatibility; unused here)
     * @return parsed LogEntryDTO
     */
    public LogEntryDTO grokLogParser(String rawLog, Long patternId) {
        LogEntryDTO logEntryDTO = LogEntryDTO.builder()
                .rawLog(rawLog)
                .metadata(new HashMap<>())
                .build();

        if (rawLog == null || rawLog.isBlank()) {
            logEntryDTO.getMetadata().put("parseFallback", "emptyLog");
            return logEntryDTO;
        }

        // 1) Try JSON first (Log4j JsonLayout commonly)
        try {
            log.info("JSON log reading....");
            JsonNode jsonNode = objectMapper.readTree(rawLog);

            // timestamp variants
            Instant ts = null;
            if (jsonNode.hasNonNull("timestamp")) {
                ts = parseTimestampToInstant(jsonNode.get("timestamp").asText(), logEntryDTO);
            } else if (jsonNode.has("instant") && jsonNode.get("instant").hasNonNull("timestamp")) {
                ts = parseTimestampToInstant(jsonNode.get("instant").get("timestamp").asText(), logEntryDTO);
            } else if (jsonNode.hasNonNull("epochMillis")) {
                ts = Instant.ofEpochMilli(jsonNode.get("epochMillis").asLong());
            } else if (jsonNode.hasNonNull("timeMillis")) {
                ts = Instant.ofEpochMilli(jsonNode.get("timeMillis").asLong());
            } else if (jsonNode.hasNonNull("time") && jsonNode.get("time").isNumber()) {
                // some layouts use "time" as epoch millis
                ts = Instant.ofEpochMilli(jsonNode.get("time").asLong());
            }

            if (ts != null) logEntryDTO.setTimestamp(ts);

            // level
            if (jsonNode.hasNonNull("level")) logEntryDTO.setLevel(jsonNode.get("level").asText());
            else if (jsonNode.hasNonNull("levelValue")) logEntryDTO.setLevel(jsonNode.get("levelValue").asText());

            // service/logger detection
            if (jsonNode.hasNonNull("serviceName")) logEntryDTO.setServiceName(jsonNode.get("serviceName").asText());
            else if (jsonNode.hasNonNull("application")) logEntryDTO.setServiceName(jsonNode.get("application").asText());
            else if (jsonNode.hasNonNull("logger")) logEntryDTO.setServiceName(jsonNode.get("logger").asText());
            else if (jsonNode.hasNonNull("loggerName")) logEntryDTO.setServiceName(jsonNode.get("loggerName").asText());

            // message: could be textual or JSON object
            if (jsonNode.has("message")) {
                JsonNode msgNode = jsonNode.get("message");
                if (msgNode.isTextual()) {
                    logEntryDTO.setMessage(msgNode.asText());
                } else {
                    // compact string for object message
                    logEntryDTO.setMessage(objectMapper.writeValueAsString(msgNode));
                    // also merge message object's top-level fields into metadata, prefix "msg."
                    msgNode.fields().forEachRemaining(f ->
                            logEntryDTO.getMetadata().put("msg." + f.getKey(), f.getValue().isValueNode() ? f.getValue().asText() : f.getValue().toString()));
                }
            } else if (jsonNode.hasNonNull("formattedMessage")) { // alternative names
                logEntryDTO.setMessage(jsonNode.get("formattedMessage").asText());
            }

            // exception/thrown
            if (jsonNode.hasNonNull("exception")) {
                logEntryDTO.setException(jsonNode.get("exception").asText());
            } else if (jsonNode.hasNonNull("thrown")) {
                logEntryDTO.setException(jsonNode.get("thrown").asText());
            } else if (jsonNode.hasNonNull("stacktrace")) {
                logEntryDTO.setException(jsonNode.get("stacktrace").asText());
            }

            // copy other top-level fields into metadata (safely)
            jsonNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                try {
                    if ("message".equals(key) || "instant".equals(key) || "timestamp".equals(key) || "exception".equals(key)) {
                        // already consumed
                        return;
                    }
                    JsonNode val = entry.getValue();
                    logEntryDTO.getMetadata().put(key, val.isValueNode() ? val.asText() : val.toString());
                } catch (Exception ex) {
                    log.debug("Failed to copy metadata key {} : {}", entry.getKey(), ex.toString());
                }
            });

            return logEntryDTO;
        } catch (Exception e) {
            // not JSON — continue to Grok/text parsing
            log.debug("Not JSON or JSON parsing failed; will try Grok/text parsing. cause={}", e.toString());
        }

        // 2) Try Grok pattern (text logs)
        try {
            log.info("GROK log reading... ");
            Match match = grok.match(rawLog);
            // Some Grok API variants require calling match.captures() or match.toMap(); capture() returns Map in some libs.
            Map<String, Object> capture = match.capture();

            if (capture == null || capture.isEmpty()) {
                log.warn("Grok did not capture any fields for log: {}", safePreview(rawLog));
                logEntryDTO.getMetadata().put("parseFallback", "grokNoCapture");
                return logEntryDTO;
            }

            // timestamp
            Object tsObj = capture.get("timestamp");
            if (tsObj != null) {
                String tsString = tsObj.toString();
                try {
                    Instant parsed = parseTimestampToInstant(tsString, logEntryDTO);
                    logEntryDTO.setTimestamp(parsed);
                } catch (Exception ex) {
                    log.debug("Failed to parse Grok timestamp '{}' : {}", tsString, ex.toString());
                }
            }

            // level, logger, message
            if (capture.containsKey("level")) logEntryDTO.setLevel(extractSingleValue(capture.get("level")));
            if (capture.containsKey("logger")) logEntryDTO.setServiceName(extractSingleValue(capture.get("logger")));
            if (capture.containsKey("message")) {
                String message = extractSingleValue(capture.get("message"));
                // if multi-line rawLog, try to capture full trailing message
                if (rawLog.contains("\n")) {
                    int idx = rawLog.indexOf(message);
                    if (idx >= 0) {
                        message = rawLog.substring(idx);
                    }
                }
                if (isStackTrace(message)) logEntryDTO.setException(message);
                else logEntryDTO.setMessage(message);
            }

            // store other keys into metadata
            capture.forEach((key, value) -> {
                if (Arrays.asList("timestamp", "level", "logger", "message").contains(key)) return;
                String v = extractSingleValue(value);
                if (v != null) logEntryDTO.getMetadata().put(key, v);
            });

            return logEntryDTO;
        } catch (Exception gre) {
            log.error("Grok parsing failed entirely for log: {} cause: {}", safePreview(rawLog), gre.toString());
            logEntryDTO.getMetadata().put("parseFallback", "grokException");
            return logEntryDTO;
        }
    }

    private String safePreview(String raw) {
        if (raw == null) return "null";
        if (raw.length() <= 200) return raw;
        return raw.substring(0, 200) + "...";
    }

    private String extractSingleValue(Object value) {
        if (value == null) return null;
        if (value instanceof List<?> list) {
            return list.isEmpty() ? null : Objects.toString(list.get(0));
        }
        return value.toString();
    }

    /**
     * Heuristic: stack trace lines generally start with "at " or "Caused by"
     */
    private boolean isStackTrace(String message) {
        if (message == null) return false;
        return message.lines().anyMatch(line -> line.trim().startsWith("at ") || line.trim().startsWith("Caused by"));
    }

    /**
     * Robust timestamp parser covering:
     *  - ISO instants with 'Z' or offset
     *  - yyyy-MM-dd HH:mm:ss,SSS  (comma)
     *  - yyyy-MM-dd HH:mm:ss.SSS  (dot)
     *  - yyyy-MM-dd HH:mm:ss      (no millis)
     *  - yyyy-MM-dd'T'HH:mm:ss[.SSS]
     *  - numeric epoch seconds (10 digits) or epoch millis (13 digits)
     *
     * Records metadata "assumedZone" when local zone is assumed.
     */
    private Instant parseTimestampToInstant(String ts, LogEntryDTO logEntryDTO) {
        if (ts == null) return Instant.now();

        String trimmed = ts.trim();

        // Numeric epoch detection WITHOUT regex
        try {
            long num = Long.parseLong(trimmed);  // will throw if not numeric
            int len = trimmed.length();

            if (len == 10) {
                // 10 digits = epoch seconds
                return Instant.ofEpochSecond(num);
            }

            if (len >= 13 && len <= 17) {
                // 13+ digits = epoch millis (Log4j timeMillis, your epochMillis)
                return Instant.ofEpochMilli(num);
            }

            // if it's numeric but odd length (unlikely): assume millis
            if (len > 10) {
                return Instant.ofEpochMilli(num);
            }

        } catch (NumberFormatException ignored) {
            // Not numeric -> do nothing
        }

        // ISO instant with Z or offset
        try {
            if (trimmed.endsWith("Z")) {
                return Instant.parse(trimmed);
            }
            // OffsetDateTime handles offsets like +05:30
            OffsetDateTime odt = OffsetDateTime.parse(trimmed);
            return odt.toInstant();
        } catch (DateTimeParseException ignored) {
        }

        // space separated with comma ms (yyyy-MM-dd HH:mm:ss,SSS)
        try {
            if (trimmed.contains(",") && trimmed.contains(" ")) {
                LocalDateTime ldt = LocalDateTime.parse(trimmed, SPACE_MS_COMMA_FORMATTER);
                if (logEntryDTO != null) logEntryDTO.getMetadata().put("assumedZone", defaultZone.toString());
                return ldt.atZone(defaultZone).toInstant();
            }
        } catch (DateTimeParseException ignored) {
        }

        // space separated with dot ms (yyyy-MM-dd HH:mm:ss.SSS)
        try {
            if (trimmed.contains(".") && trimmed.contains(" ")) {
                LocalDateTime ldt = LocalDateTime.parse(trimmed, SPACE_MS_DOT_FORMATTER);
                if (logEntryDTO != null) logEntryDTO.getMetadata().put("assumedZone", defaultZone.toString());
                return ldt.atZone(defaultZone).toInstant();
            }
        } catch (DateTimeParseException ignored) {
        }

        // space separated seconds-only (yyyy-MM-dd HH:mm:ss)
        try {
            if (trimmed.contains(" ") && trimmed.split(" ").length >= 2 && trimmed.chars().filter(ch -> ch == ':').count() == 2) {
                LocalDateTime ldt = LocalDateTime.parse(trimmed, SPACE_SECONDS_FORMATTER);
                if (logEntryDTO != null) logEntryDTO.getMetadata().put("assumedZone", defaultZone.toString());
                return ldt.atZone(defaultZone).toInstant();
            }
        } catch (DateTimeParseException ignored) {
        }

        // ISO-like without zone (yyyy-MM-dd'T'HH:mm:ss[.SSS])
        try {
            if (trimmed.contains("T")) {
                try {
                    LocalDateTime ldtIso = LocalDateTime.parse(trimmed, ISO_WITH_MILLIS);
                    if (logEntryDTO != null) logEntryDTO.getMetadata().put("assumedZone", defaultZone.toString());
                    return ldtIso.atZone(defaultZone).toInstant();
                } catch (DateTimeParseException ignored) {
                }
                try {
                    LocalDateTime ldtIso = LocalDateTime.parse(trimmed, ISO_NO_MILLIS);
                    if (logEntryDTO != null) logEntryDTO.getMetadata().put("assumedZone", defaultZone.toString());
                    return ldtIso.atZone(defaultZone).toInstant();
                } catch (DateTimeParseException ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        // last resort: log and return now (and mark fallback)
        log.error("Unable to parse timestamp: '{}'. Using Instant.now() as fallback.", ts);
        if (logEntryDTO != null) logEntryDTO.getMetadata().put("parseFallback", "timestampParseFailed");
        return Instant.now();
    }
}

