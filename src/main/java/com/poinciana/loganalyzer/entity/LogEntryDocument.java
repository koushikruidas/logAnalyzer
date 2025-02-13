package com.poinciana.loganalyzer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDateTime;

@Document(indexName = "log_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntryDocument {

    @Id
    private String id;
    private LocalDateTime timestamp;
    private String level;
    private String serviceName;
    private String message;
    private String exception;
    private String rawLog;
}