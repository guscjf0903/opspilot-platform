package com.opspilot.cluster.domain;

public record ClusterSummary(
        String id,
        String name,
        String provider,
        ClusterConnectionStatus status,
        String endpoint
) {
}
