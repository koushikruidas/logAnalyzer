package com.poinciana.loganalyzer.controller;

import com.poinciana.loganalyzer.service.interfaces.TopicIndexMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/topic-index")
@RequiredArgsConstructor
public class TopicIndexMapController {

    private final TopicIndexMapService topicIndexMapService;

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshMap() {
        topicIndexMapService.refreshMap();
        return ResponseEntity.ok("Topic-index map refreshed successfully.");
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> getMap() {
        return ResponseEntity.ok(topicIndexMapService.getCurrentMap());
    }
}
