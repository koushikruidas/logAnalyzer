package com.poinciana.loganalyzer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

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

    private boolean isDefault; // If true, use this when no patternId is provided

    @ElementCollection
    @CollectionTable(name = "log_pattern_mappings", joinColumns = @JoinColumn(name = "log_pattern_id"))
    @MapKeyColumn(name = "field_name") // Key: log field (timestamp, level, etc.)
    @Column(name = "group_position") // Value: Regex group position
    private Map<String, Integer> fieldMappings;
}

