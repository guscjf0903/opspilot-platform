package com.opspilot.kubernetes.domain;

import java.time.Instant;

public record DeploymentSummary(
        String kind,
        String namespace,
        String name,
        ResourceStatus status,
        String reason,
        String message,
        Instant lastUpdatedAt,
        int desiredReplicas,
        int availableReplicas,
        int readyReplicas,
        int updatedReplicas
) {
}
