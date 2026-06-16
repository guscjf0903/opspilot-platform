package com.opspilot.action.domain;

import java.time.Instant;
import java.util.UUID;

public record ActionApprovalRequest(
        UUID id,
        UUID auditLogId,
        String clusterId,
        String namespace,
        String targetKind,
        String targetName,
        ActionType actionType,
        String requester,
        UserRole requesterRole,
        ActionStatus status,
        Instant requestedAt,
        String decidedBy,
        Instant decidedAt,
        String decisionReason
) {
}
