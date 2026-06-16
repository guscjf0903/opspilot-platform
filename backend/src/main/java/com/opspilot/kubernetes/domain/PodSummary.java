package com.opspilot.kubernetes.domain;

import java.time.Instant;
import java.util.List;

public record PodSummary(
        String kind,
        String namespace,
        String name,
        ResourceStatus status,
        String reason,
        String message,
        Instant lastUpdatedAt,
        String phase,
        String nodeName,
        int restartCount,
        List<String> images
) {
}
