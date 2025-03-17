package com.poinciana.loganalyzer.service.interfaces;

import com.poinciana.loganalyzer.model.ElasticAdminDTO;

public interface ElasticAdminService {
    void createIndex(ElasticAdminDTO elasticAdminDTO);
}
