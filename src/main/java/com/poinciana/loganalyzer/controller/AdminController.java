package com.poinciana.loganalyzer.controller;

import com.poinciana.loganalyzer.entity.ApiKey;
import com.poinciana.loganalyzer.model.ApiKeyDTO;
import com.poinciana.loganalyzer.model.ElasticAdminDTO;
import com.poinciana.loganalyzer.model.KafkaAdminDTO;
import com.poinciana.loganalyzer.repository.ApiKeyRepository;
import com.poinciana.loganalyzer.service.interfaces.ElasticAdminService;
import com.poinciana.loganalyzer.service.interfaces.KafkaAdminService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final ApiKeyRepository apiKeyRepository;
    private final KafkaAdminService kafkaAdminService;
    private final ElasticAdminService elasticAdminService;
    private final ModelMapper modelMapper;

    @PostMapping("/register")
    public ResponseEntity<String> registerApplication(@RequestBody ApiKeyDTO apiKeyDTO) {
        String apiKey = UUID.randomUUID().toString();
        String kafkaTopic = "logs." + apiKeyDTO.getApplicationName().toLowerCase();
        String elasticIndex = apiKeyDTO.getApplicationName().toLowerCase() + "-logs";

        // Convert DTO to entity
        ApiKey newApiKey = modelMapper.map(apiKeyDTO, ApiKey.class);
        newApiKey.setApiKey(apiKey);
        newApiKey.setKafkaTopic(kafkaTopic);
        newApiKey.setElasticIndex(elasticIndex);
        newApiKey.setActive(true);
        apiKeyRepository.save(newApiKey);

        // Create Kafka Topic
        KafkaAdminDTO kafkaDTO = new KafkaAdminDTO();
        kafkaDTO.setTopicName(kafkaTopic);
        kafkaAdminService.createTopic(kafkaDTO);

        // Create Elasticsearch Index
        ElasticAdminDTO elasticDTO = new ElasticAdminDTO();
        elasticDTO.setIndexName(elasticIndex);
        elasticAdminService.createIndex(elasticDTO);

        return ResponseEntity.ok("✅ Registered " + apiKeyDTO.getApplicationName() + " -> API Key: " + apiKey);
    }

    /**
     * Get application details by API Key or appName & orgName
     */

    @GetMapping("/application/details")
    public ResponseEntity<?> getApplicationDetails(
            @RequestParam(required = false) String apiKey,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) String orgName) {

        Optional<ApiKey> apiKeyDetails;

        if (apiKey != null) {
            apiKeyDetails = apiKeyRepository.findByApiKey(apiKey);
        } else if (appName != null && orgName != null) {
            apiKeyDetails = apiKeyRepository.findByApplicationNameAndOrganizationName(appName, orgName);
        } else {
            return ResponseEntity.badRequest().body("❌ Provide either apiKey or both appName & orgName");
        }

        if (apiKeyDetails.isPresent()) {
            return ResponseEntity.ok(apiKeyDetails.get()); // Return ApiKey on success
        } else {
            // Return an error object or a String with an error status
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("API Key not found");
        }
    }


    /**
     * List all registered applications.
     */
    @GetMapping("/applications")
    public ResponseEntity<List<ApiKey>> getAllApplications() {
        return ResponseEntity.ok(apiKeyRepository.findAll());
    }
}
