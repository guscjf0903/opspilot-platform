package com.opspilot.cost.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;

public record WorkloadCost(
        String namespace,
        String kind,
        String name,
        ResourceStatus status,
        String currency,
        double estimatedMonthlyCost,
        double cpuMonthlyCost,
        double memoryMonthlyCost,
        Double cpuRequestCores,
        Long memoryRequestBytes,
        Double cpuUsageCores,
        Long memoryUsageBytes,
        Double cpuUsagePercent,
        Double memoryUsagePercent,
        boolean metricsAvailable,
        String metricsReason
) {
}
