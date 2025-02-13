package com.poinciana.loganalyzer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "log_patterns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pattern; // Custom regex pattern

    private boolean isDefault; // Marks system-defined patterns
}

