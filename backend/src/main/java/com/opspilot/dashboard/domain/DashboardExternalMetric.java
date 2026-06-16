package com.opspilot.dashboard.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;

public record DashboardExternalMetric(
        String name,
        Double cpuUsageCores,
        Long memoryUsageBytes,
        Double cpuUsagePercent,
        Double memoryUsagePercent,
        Double value,
        String unit,
        ResourceStatus status,
        String description
) {

    public DashboardExternalMetric(
            String name,
            Double cpuUsageCores,
            Long memoryUsageBytes,
            Double cpuUsagePercent,
            Double memoryUsagePercent
    ) {
        this(name, cpuUsageCores, memoryUsageBytes, cpuUsagePercent, memoryUsagePercent, null, null, null, null);
    }

    public static DashboardExternalMetric value(
            String name,
            double value,
            String unit,
            ResourceStatus status,
            String description
    ) {
        return new DashboardExternalMetric(name, null, null, null, null, value, unit, status, description);
    }
}
