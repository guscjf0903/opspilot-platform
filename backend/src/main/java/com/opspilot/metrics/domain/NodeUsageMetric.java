package com.opspilot.metrics.domain;

public record NodeUsageMetric(
        String nodeName,
        Double cpuUsageCores,
        Long memoryUsageBytes,
        Double cpuUsagePercent,
        Double memoryUsagePercent
) {
}
