package com.poinciana.loganalyzer.controller;

import com.poinciana.loganalyzer.config.LogAlertConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logs/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final LogAlertConfig logAlertConfig;

    @GetMapping("/config")
    public ResponseEntity<LogAlertConfig> getAlertConfig() {
        return ResponseEntity.ok(logAlertConfig);
    }
}