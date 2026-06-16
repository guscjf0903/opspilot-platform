package com.opspilot.cluster.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ClusterConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    DEGRADED;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }
}
