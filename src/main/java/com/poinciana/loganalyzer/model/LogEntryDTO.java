package com.poinciana.loganalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntryDTO {
    private String id;
    private String level;
    private String serviceName;
    private String message;
    private LocalDateTime timestamp;
    private String exception;
    private String hostName;
    private String hostIp;
    private String rawLog;
    private String indexName;
    private Map<String, Object> metadata;
}
