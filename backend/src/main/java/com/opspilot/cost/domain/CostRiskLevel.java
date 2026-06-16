package com.opspilot.cost.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CostRiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }
}
