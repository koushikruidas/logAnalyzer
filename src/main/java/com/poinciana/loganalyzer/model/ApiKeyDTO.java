package com.poinciana.loganalyzer.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyDTO {

    private String organizationName;
    private String applicationName;
    private String apiKey;
    private String kafkaTopic;
    private String elasticIndex;
    private boolean active;
}