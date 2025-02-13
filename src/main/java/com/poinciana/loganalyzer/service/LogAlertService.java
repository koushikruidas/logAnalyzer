package com.poinciana.loganalyzer.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.poinciana.loganalyzer.config.LogAlertConfig;
import com.poinciana.loganalyzer.config.SchedulingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogAlertService {

    private final ElasticsearchClient elasticsearchClient;
    private final LogAlertConfig logAlertConfig;
    private SchedulingConfig schedulingConfig;
    @Value("${log.alert.time-window-minutes:10}") // Get the value in minutes
    private int timeWindowMinutes;

    @Value("${elasticsearch.index}")
    private String index;

    /**
     * Checks for error spikes every X minutes.
     */
    @Scheduled(fixedRateString = "#{@schedulingConfig.getFixedRate()}")
    public void checkForErrorSpikes() throws IOException {
        int errorCount = getErrorCountInLastXMinutes(logAlertConfig.getAlertTimeWindowMinutes());

        if (errorCount >= logAlertConfig.getErrorThreshold()) {
            log.warn("🚨 ALERT: High error rate detected! {} errors in last {} minutes.",
                    errorCount, logAlertConfig.getAlertTimeWindowMinutes());

            sendWebhookAlert(errorCount);
        }
    }

    /**
     * Queries Elasticsearch for the number of ERROR logs in the last X minutes.
     */
    private int getErrorCountInLastXMinutes(int minutes) throws IOException {
        long now = System.currentTimeMillis();
        long from = now - Duration.ofMinutes(minutes).toMillis();
        SearchRequest request = new SearchRequest.Builder()
                .index(index)
                .size(0)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m.match(t -> t.field("level.keyword").query("ERROR")))
                                .filter(f -> f.range(r -> r
                                        .untyped(ut -> ut  // Use untyped() for generic range queries
                                                .field("timestamp") // Field is set here
                                                .gte(JsonData.of(from))
                                                .lte(JsonData.of(now))
                                        )
                                ))
                        )
                )
                .aggregations("error_count", a -> a
                        .valueCount(vc -> vc.field("level.keyword"))
                )
                .build();

        SearchResponse<Void> response = elasticsearchClient.search(request, Void.class);

        // ✅ FIX: Properly handle double conversion
        double countValue = response.aggregations().get("error_count").valueCount().value();
        return (int) Math.round(countValue);
    }

    /**
     * Sends an alert via webhook if configured.
     */
    private void sendWebhookAlert(int errorCount) {
        if (logAlertConfig.getWebhookUrl() != null && !logAlertConfig.getWebhookUrl().isEmpty()) {
            log.info("🔔 Sending alert webhook to {}", logAlertConfig.getWebhookUrl());
            // TODO: Implement webhook HTTP call (future enhancement)
        }
    }

    // This method is called by the SpEL expression
    public long getFixedRate() {
        return (long) timeWindowMinutes * 60 * 1000; // Calculate milliseconds
    }
}
