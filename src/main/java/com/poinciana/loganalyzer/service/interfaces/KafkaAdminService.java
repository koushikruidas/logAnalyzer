package com.poinciana.loganalyzer.service.interfaces;

import com.poinciana.loganalyzer.model.KafkaAdminDTO;

public interface KafkaAdminService {
    void createTopic(KafkaAdminDTO kafkaAdminDTO);
}
