package com.poinciana.loganalyzer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntryDocument {

    @Id
    private String id;
    private LocalDateTime timestamp;

    @Field(type = FieldType.Text)
    private String level;

    @Field(type = FieldType.Text)
    private String serviceName;

    @Field(type = FieldType.Text)
    private String message;

    @Field(type = FieldType.Text)
    private String exception;

    @Field(type = FieldType.Text)
    private String rawLog;

    @Field(type = FieldType.Text)
    private String hostName;

    @Field(type = FieldType.Text)
    private String hostIp;

    /**
     * This field will store log-specific fields dynamically
     * (e.g., IP Address, Thread Name, HTTP Method, etc.)
     */
    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;
}