package com.opspilot.trends.domain;

import java.time.Instant;

public record TrendSnapshotCollectionResult(
        String clusterId,
        int kubernetesSnapshotCount,
        int costSnapshotCount,
        Instant collectedAt
) {
}
