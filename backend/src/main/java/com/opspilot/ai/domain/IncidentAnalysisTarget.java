package com.opspilot.ai.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;

public record IncidentAnalysisTarget(
        String namespace,
        String kind,
        String name,
        ResourceStatus status,
        String reason,
        String message,
        Instant lastUpdatedAt
) {
}
