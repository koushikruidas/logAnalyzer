package com.poinciana.loganalyzer.repository;

import com.poinciana.loganalyzer.entity.LogEntryDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogEntryElasticsearchRepository extends ElasticsearchRepository<LogEntryDocument, String> {

}