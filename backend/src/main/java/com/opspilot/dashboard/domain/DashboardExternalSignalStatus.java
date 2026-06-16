package com.opspilot.dashboard.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DashboardExternalSignalStatus {
    AVAILABLE,
    UNAVAILABLE;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }
}
