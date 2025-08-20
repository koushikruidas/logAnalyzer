package com.poinciana.loganalyzer.scheduler;

import com.poinciana.loganalyzer.service.interfaces.TopicIndexMapService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TopicIndexSyncScheduler {

    private final TopicIndexMapService topicIndexMapService;

    @PostConstruct
    public void init() {
        topicIndexMapService.refreshMap(); // Load at startup
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000) // every 5 mins
    public void refreshPeriodically() {
        topicIndexMapService.refreshMap();
    }
}
