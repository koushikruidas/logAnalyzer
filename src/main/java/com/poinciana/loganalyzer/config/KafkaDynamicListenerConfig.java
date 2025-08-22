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

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class KafkaDynamicListenerConfig {

    private final KafkaTopicResolver kafkaTopicResolver;
    private final KafkaGroupResolver kafkaGroupResolver;
    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaLogConsumer kafkaLogConsumer;

    @PostConstruct
    public void registerDynamicListeners() {
        for (String orgId : kafkaGroupResolver.getOrgIds()) {
            String groupId = kafkaGroupResolver.getGroupForOrg(orgId);
            List<String> topics = kafkaTopicResolver.getTopicsForOrg(orgId);
            if (topics.isEmpty()) continue;

            ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory);
            factory.setConcurrency(3);

            ConcurrentMessageListenerContainer<String, String> container =
                    factory.createContainer(topics.toArray(new String[0]));
            container.getContainerProperties().setGroupId(groupId);
            container.getContainerProperties().setMessageListener(
                    (MessageListener<String, String>) record -> {
                        kafkaLogConsumer.consumeLogs(List.of(record), null);
                    }
            );
            container.start();
        }
    }
}