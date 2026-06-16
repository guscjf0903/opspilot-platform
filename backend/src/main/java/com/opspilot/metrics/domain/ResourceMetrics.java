package com.opspilot.metrics.domain;

import java.time.Instant;

public record ResourceMetrics(
        MetricsAvailabilityStatus status,
        String reason,
        String clusterId,
        String namespace,
        String kind,
        String name,
        Instant collectedAt,
        int rangeMinutes,
        MetricSeries cpu,
        MetricSeries memory,
        ResourceMetricSummary summary
) {

    public static ResourceMetrics available(
            String clusterId,
            String namespace,
            String kind,
            String name,
            Instant collectedAt,
            MetricQueryWindow window,
            MetricSeries cpu,
            MetricSeries memory,
            ResourceMetricSummary summary
    ) {
        return new ResourceMetrics(
                MetricsAvailabilityStatus.AVAILABLE,
                "PROMETHEUS_CONNECTED",
                clusterId,
                namespace,
                kind,
                name,
                collectedAt,
                window.rangeMinutes(),
                cpu,
                memory,
                summary
        );
    }

    public static ResourceMetrics unavailable(
            String reason,
            String clusterId,
            String namespace,
            String kind,
            String name,
            Instant collectedAt,
            MetricQueryWindow window
    ) {
        return new ResourceMetrics(
                MetricsAvailabilityStatus.UNAVAILABLE,
                reason,
                clusterId,
                namespace,
                kind,
                name,
                collectedAt,
                window.rangeMinutes(),
                MetricSeries.empty("cpu", "cores"),
                MetricSeries.empty("memory", "bytes"),
                ResourceMetricSummary.empty()
        );
    }
}
