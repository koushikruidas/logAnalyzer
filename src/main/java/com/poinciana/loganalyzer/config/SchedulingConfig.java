package com.poinciana.loganalyzer.config;

import com.poinciana.loganalyzer.service.LogAlertService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling; // Important!
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;

@EnableScheduling // Enable scheduling
@Configuration // Mark as a configuration class
public class SchedulingConfig {
    @Value("${log.alert.time-window-minutes:10}")
    private int timeWindowMinutes;

    public long getFixedRate() {
        return (long) timeWindowMinutes * 60 * 1000;
    }
}