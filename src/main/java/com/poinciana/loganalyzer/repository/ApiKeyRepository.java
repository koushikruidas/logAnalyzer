package com.poinciana.loganalyzer.repository;

import com.poinciana.loganalyzer.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByApiKey(String apiKey);

    Optional<ApiKey> findByApplicationNameAndOrganizationName(String appName, String orgName);
}
