package com.poinciana.loganalyzer.config;

import com.poinciana.loganalyzer.listener_service.KafkaLogConsumer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class KafkaDynamicListenerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaDynamicListenerConfig.class);

    private final KafkaTopicResolver kafkaTopicResolver;
    private final KafkaGroupResolver kafkaGroupResolver;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaLogConsumer kafkaLogConsumer;

    @PostConstruct
    public void registerDynamicListeners() {
        // List to collect info about registered listeners for consolidated logging
        List<String> listenerSummaries = new ArrayList<>();
        int listenerCount = 0;
        for (String orgId : kafkaGroupResolver.getOrgIds()) {
            log.info("Setting up Kafka listener for orgId={}", orgId);
            String groupId = kafkaGroupResolver.getGroupForOrg(orgId);
            log.info("Using groupId={}", groupId);
            List<String> topics = kafkaTopicResolver.getTopicsForOrg(orgId);
            log.info("Subscribing to topics={} for orgId={}", topics, orgId);
            if (topics.isEmpty()) continue;

            ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory);
            factory.setConcurrency(3);

            ConcurrentMessageListenerContainer<String, String> container =
                    factory.createContainer(topics.toArray(new String[0]));
            container.getContainerProperties().setGroupId(groupId);

            // --- MessageListener with automatic ack (default) ---
            // Uncomment this block if you want auto-acknowledgment (Spring will commit offsets automatically)
            // container.getContainerProperties().setMessageListener(
            //         (MessageListener<String, String>) record -> {
            //             kafkaLogConsumer.consumeLogs(List.of(record), null);
            //         }
            // );

            // --- AcknowledgingMessageListener for manual ack ---
            // Use this block if you want to manually acknowledge records
            container.getContainerProperties().setMessageListener(
                    (org.springframework.kafka.listener.AcknowledgingMessageListener<String, String>) (record, acknowledgment) -> {
                        kafkaLogConsumer.consumeLogs(List.of(record), acknowledgment);
                    }
            );

            // Start the container
            container.start();
            log.info("Kafka listener container started for orgId={}, groupId={}, topics={}", orgId, groupId, topics);

            // Add a ConsumerRebalanceListener to log partition assignment
            container.getContainerProperties().setConsumerRebalanceListener(new org.apache.kafka.clients.consumer.ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<org.apache.kafka.common.TopicPartition> partitions) {
                    log.info("Partitions revoked for orgId={}, groupId={}: {}", orgId, groupId, partitions);
                }
                @Override
                public void onPartitionsAssigned(Collection<org.apache.kafka.common.TopicPartition> partitions) {
                    log.info("Partitions assigned for orgId={}, groupId={}: {}", orgId, groupId, partitions);
                }
            });

            // Collect info for consolidated log
            listenerSummaries.add(String.format("orgId=%s, groupId=%s, topics=%s", orgId, groupId, topics));
            listenerCount++;
        }
        // Consolidated log of all started listeners
        if (!listenerSummaries.isEmpty()) {
            log.info("Started {} Kafka listeners:\n{}", listenerCount, String.join("\n", listenerSummaries));
        } else {
            log.warn("No Kafka listeners started (no topics found for any orgId)");
        }
    }
}