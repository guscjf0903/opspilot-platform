package com.opspilot.metrics.domain;

public record ResourceMetricSummary(
        Double cpuUsageCores,
        Long memoryUsageBytes,
        Double cpuRequestCores,
        Long memoryRequestBytes,
        Double cpuCapacityCores,
        Long memoryCapacityBytes,
        Double cpuUsagePercent,
        Double memoryUsagePercent
) {

    public static ResourceMetricSummary empty() {
        return new ResourceMetricSummary(null, null, null, null, null, null, null, null);
    }
}
