package com.opspilot.kubernetes.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum ResourceStatus {
    HEALTHY,
    WARNING,
    CRITICAL,
    UNKNOWN;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static ResourceStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        try {
            return ResourceStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}
