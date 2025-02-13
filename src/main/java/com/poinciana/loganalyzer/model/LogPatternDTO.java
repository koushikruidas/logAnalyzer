package com.poinciana.loganalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogPatternDTO {
    private Long id;
    private String pattern;
    private boolean isDefault;
    private Map<String, Integer> fieldMappings;
}