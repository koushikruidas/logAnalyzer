package com.poinciana.loganalyzer.service.interfaces;

import java.util.Map;

public interface TopicIndexMapService {
    String resolveIndex(String topic);
    void refreshMap();
    Map<String, String> getCurrentMap();
}
