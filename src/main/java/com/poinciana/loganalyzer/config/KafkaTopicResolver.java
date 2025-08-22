package com.poinciana.loganalyzer.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class KafkaTopicResolver {
    @Value("${ORG_ID}")
    private String orgId;
    @Value("${spring.kafka.properties.sasl.username}")
    private String saslUsername;

    @Value("${spring.kafka.properties.sasl.password}")
    private String saslPassword;

    @Value("${spring.kafka.properties.security.protocol}")
    private String securityProtocol;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    public List<String> getTopics() {
        Map<String, Object> adminProps = new HashMap<>(kafkaAdmin.getConfigurationProperties());

        adminProps.put("security.protocol", securityProtocol);
        adminProps.put("sasl.mechanism", System.getenv("KAFKA_SASL_ENABLED_MECHANISMS"));

        // Inject JAAS config inline
        adminProps.put("sasl.jaas.config",
                "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                        "username=\"" + saslUsername + "\" password=\"" + saslPassword + "\";");
        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            Set<String> orgIds = Arrays.stream(orgId.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());

            return adminClient.listTopics().names().get().stream()
                    .filter(topic -> orgIds.stream().anyMatch(org -> topic.startsWith(org + "_")))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch topics", e);
        }
    }

    public List<String> getTopicsForOrg(String orgId) {
        Map<String, Object> adminProps = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        adminProps.put("security.protocol", securityProtocol);
        adminProps.put("sasl.mechanism", System.getenv("KAFKA_SASL_ENABLED_MECHANISMS"));
        adminProps.put("sasl.jaas.config",
                "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                        "username=\"" + saslUsername + "\" password=\"" + saslPassword + "\";");
        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            return adminClient.listTopics().names().get().stream()
                    .filter(topic -> topic.startsWith(orgId + "_"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch topics", e);
        }
    }
}
