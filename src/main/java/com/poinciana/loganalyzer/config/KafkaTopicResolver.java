package com.poinciana.loganalyzer.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class KafkaTopicResolver {
    @Value("${ORG_ID:logs}")
    private String orgId;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    public List<String> getTopics() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            return adminClient.listTopics().names().get().stream()
                    .filter(topic -> topic.startsWith(orgId + "_"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch topics", e);
        }
    }
}
