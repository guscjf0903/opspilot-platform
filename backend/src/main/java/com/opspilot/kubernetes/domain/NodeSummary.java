package com.opspilot.kubernetes.domain;

import java.time.Instant;

public record NodeSummary(
        String kind,
        String name,
        ResourceStatus status,
        String reason,
        String message,
        Instant lastUpdatedAt,
        boolean unschedulable,
        String kubeletVersion
) {
}
