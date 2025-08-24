package com.poinciana.loganalyzer.service;

import com.poinciana.loganalyzer.service.interfaces.TopicIndexClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicIndexClientImpl implements TopicIndexClient {

    @Value("${logadmin.url}")
    private String logAdminUrl;

    private final RestTemplate restTemplate;

    @Override
    public Map<String, String> fetchTopicIndexMap() {
        String url = logAdminUrl + "/api/admin/topic-index-map";
        ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        log.info("Fetched topic-index map from logAdmin: {}", response.getStatusCode());
        Map<String, String> map = response.getBody() != null ? response.getBody() : new HashMap<>();
        log.debug("Topic-Index map content: {}", map);
        return map;
    }
}
