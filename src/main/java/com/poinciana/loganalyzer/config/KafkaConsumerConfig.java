package com.poinciana.loganalyzer.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    @Value(("${spring.kafka.consumer.pollType}"))
    private String pollType;
    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBroker;
    @Value("${spring.kafka.consumer.maxRecords}")
    private int maxRecods;
    @Value("${spring.kafka.consumer.concurrency}")
    private int concurrency;

    @Bean
    public Map<String, Object> kafkaListerConfig(){
        log.info("Inside kafkaListenerConfig()");
        log.info("group: "+groupId);
        log.info("PollType: "+pollType);

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBroker );
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, pollType);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxRecods);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return properties;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        log.info("Inside consumerFactory()");
        return new DefaultKafkaConsumerFactory<>(kafkaListerConfig());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String,String>> kafkaListenerContainerFactory() {
        log.info("Inside kafkaListenerContainerFactory()");
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(concurrency);
        factory.setBatchListener(true);
        return factory;
    }
}
