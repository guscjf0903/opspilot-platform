package com.opspilot.cost.domain;

import java.util.Map;

public record CostRecommendation(
        String id,
        CostRecommendationType type,
        String namespace,
        String targetKind,
        String targetName,
        String title,
        Map<String, String> current,
        Map<String, String> recommendation,
        String currency,
        double estimatedMonthlySaving,
        CostRiskLevel risk,
        double confidence,
        String reason
) {
}
