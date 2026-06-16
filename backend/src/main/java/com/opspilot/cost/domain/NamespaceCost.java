package com.opspilot.cost.domain;

public record NamespaceCost(
        String namespace,
        String currency,
        double estimatedMonthlyCost,
        double estimatedMonthlySaving,
        int workloadCount,
        int recommendationCount
) {
}
