package com.poinciana.loganalyzer.controller;

import com.poinciana.loganalyzer.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/logs/aggregation")
@RequiredArgsConstructor
public class AggregationController {

    private final LogService logService;

    @GetMapping("/count-by-level")
    public ResponseEntity<Map<String, Long>> getLogCountByLevel() throws IOException {
        return ResponseEntity.ok(logService.getLogCountByLevel());
    }

    @GetMapping("/count-by-service")
    public ResponseEntity<Map<String, Long>> getLogCountByService() throws IOException {
        return ResponseEntity.ok(logService.getLogCountByService());
    }

    @GetMapping("/count-by-date")
    public ResponseEntity<Map<String, Long>> getLogCountByDate() throws IOException {
        return ResponseEntity.ok(logService.getLogCountByDate());
    }
}