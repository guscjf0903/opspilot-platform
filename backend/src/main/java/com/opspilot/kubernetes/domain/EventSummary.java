package com.opspilot.kubernetes.domain;

import java.time.Instant;

public record EventSummary(
        String kind,
        String namespace,
        String name,
        ResourceStatus status,
        String reason,
        String message,
        Instant lastUpdatedAt,
        String type,
        String involvedKind,
        String involvedName,
        int count
) {
}
