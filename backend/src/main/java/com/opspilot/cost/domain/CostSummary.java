package com.opspilot.cost.domain;

import java.time.Instant;

public record CostSummary(
        String clusterId,
        String currency,
        double estimatedMonthlyCost,
        double estimatedMonthlySaving,
        int namespaceCount,
        int workloadCount,
        int recommendationCount,
        String estimationMode,
        Instant collectedAt
) {
}
