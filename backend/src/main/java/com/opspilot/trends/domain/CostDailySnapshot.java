package com.opspilot.trends.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CostDailySnapshot(
        UUID id,
        String clusterId,
        String namespace,
        String workloadKind,
        String workloadName,
        ResourceStatus status,
        String currency,
        double estimatedDailyCost,
        double estimatedMonthlyCost,
        double cpuMonthlyCost,
        double memoryMonthlyCost,
        double estimatedMonthlySaving,
        String estimationMode,
        LocalDate snapshotDate,
        Instant collectedAt
) {
}
