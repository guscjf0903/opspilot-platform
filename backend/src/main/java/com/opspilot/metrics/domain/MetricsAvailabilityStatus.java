package com.opspilot.metrics.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MetricsAvailabilityStatus {
    AVAILABLE,
    UNAVAILABLE;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }
}
