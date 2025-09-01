package com.poinciana.loganalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class KafkaGroupResolver {
    @Value("${ORG_ID}")
    private String orgId;

    public Set<String> getOrgIds() {
        return Arrays.stream(orgId.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    public String getGroupForOrg(String orgId) {
        return orgId + "_consumer_group";
    }
}
