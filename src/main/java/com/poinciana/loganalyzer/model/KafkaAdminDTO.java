package com.poinciana.loganalyzer.model;

import lombok.Data;

@Data
public class KafkaAdminDTO {
    private String topicName;
    private int partitions = 3; // Default partitions
    private short replicationFactor = 1; // Default replication
}
