package com.opspilot.ai.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;

public record IncidentEvidence(
        String id,
        String type,
        String title,
        String message,
        ResourceStatus status,
        Instant timestamp
) {
}
