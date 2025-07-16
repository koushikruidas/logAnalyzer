package com.poinciana.loganalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

//@Component
public class KafkaGroupResolver {
    @Value("${ORG_ID}")
    private String orgId;

    public String getGroupId() {
        return orgId+"_group";
    }
}
