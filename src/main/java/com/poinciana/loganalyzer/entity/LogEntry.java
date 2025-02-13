package com.poinciana.loganalyzer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private String level; // ERROR, WARN, INFO, DEBUG
    private String serviceName;
    private String message;
    private String exception; // Stores stack traces if any

    @Lob
    private String rawLog; // Stores original log entry
}

