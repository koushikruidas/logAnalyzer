package com.poinciana.loganalyzer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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
}