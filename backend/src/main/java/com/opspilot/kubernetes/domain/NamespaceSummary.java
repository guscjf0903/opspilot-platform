package com.opspilot.kubernetes.domain;

import java.time.Instant;

public record NamespaceSummary(
        String kind,
        String name,
        ResourceStatus status,
        String reason,
        String message,
        Instant lastUpdatedAt
) {
}
