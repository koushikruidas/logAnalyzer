package com.poinciana.loganalyzer.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class KafkaTopicResolver {
    @Value("${ORG_ID:logs}")
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
        adminProps.put("sasl.mechanism", "PLAIN");

        // Inject JAAS config inline
        adminProps.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
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
