package com.opspilot.cost.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CostRecommendationType {
    CPU_RIGHTSIZING,
    MEMORY_RIGHTSIZING,
    IDLE_WORKLOAD;

    @JsonValue
    public String value() {
        return name();
    }
}
