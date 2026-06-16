package com.opspilot.metrics.domain;

import java.util.List;

public record NodeUsageSnapshot(
        MetricsAvailabilityStatus status,
        String reason,
        List<NodeUsageMetric> nodes
) {

    public static NodeUsageSnapshot available(List<NodeUsageMetric> nodes) {
        return new NodeUsageSnapshot(MetricsAvailabilityStatus.AVAILABLE, "PROMETHEUS_CONNECTED", nodes);
    }

    public static NodeUsageSnapshot unavailable(String reason) {
        return new NodeUsageSnapshot(MetricsAvailabilityStatus.UNAVAILABLE, reason, List.of());
    }
}
