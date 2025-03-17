package com.poinciana.loganalyzer.service;

import com.poinciana.loganalyzer.entity.ApiKey;
import com.poinciana.loganalyzer.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ApiKeyService {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    public boolean isValidApiKey(String apiKey) {
        Optional<ApiKey> key = apiKeyRepository.findByApiKey(apiKey);
        return key.isPresent() && key.get().isActive();
    }

    public Optional<ApiKey> getApiKeyDetails(String apiKey) {
        return apiKeyRepository.findByApiKey(apiKey);
    }
}
