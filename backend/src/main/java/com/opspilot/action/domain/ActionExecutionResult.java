package com.opspilot.action.domain;

import java.time.Instant;
import java.util.UUID;

public record ActionExecutionResult(
        UUID auditLogId,
        UUID approvalId,
        ActionStatus status,
        ActionPreview preview,
        String message,
        Instant executedAt
) {
}
