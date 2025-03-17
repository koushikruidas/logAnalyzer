package com.poinciana.loganalyzer.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "api_keys")
@Data
public class ApiKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String organizationName; // Example: "Acme Corp"
    private String applicationName;  // Example: "InventoryService"
    private String apiKey;  // Unique API Key
    private String kafkaTopic; // Example: "logs.inventory-service"
    private String elasticIndex; // Example: "inventory-service-logs"
    private boolean active;  // Can be deactivated if needed
}
