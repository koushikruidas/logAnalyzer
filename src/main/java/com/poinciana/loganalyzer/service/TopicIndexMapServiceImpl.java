package com.poinciana.loganalyzer.service;

import com.poinciana.loganalyzer.service.interfaces.TopicIndexMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopicIndexMapServiceImpl implements TopicIndexMapService {

    private final TopicIndexClientImpl topicIndexClientImpl;
    private final Map<String, String> topicIndexMap = new ConcurrentHashMap<>();

    @Override
    public String resolveIndex(String topic) {
        return topicIndexMap.getOrDefault(topic, topic); // fallback: topic = index
    }

    @Override
    public void refreshMap() {
        try {
            Map<String, String> updatedMap = topicIndexClientImpl.fetchTopicIndexMap();
            topicIndexMap.clear();
            topicIndexMap.putAll(updatedMap);
        } catch (Exception e) {
            // Handle/log failure without crashing
            log.error("Failed to refresh topic-index map from logAdmin: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, String> getCurrentMap() {
        return Collections.unmodifiableMap(topicIndexMap);
    }
}
