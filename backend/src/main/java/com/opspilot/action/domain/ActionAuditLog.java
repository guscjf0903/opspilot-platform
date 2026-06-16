package com.opspilot.action.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ActionAuditLog(
        UUID id,
        String clusterId,
        String namespace,
        String targetKind,
        String targetName,
        ActionType actionType,
        String actor,
        UserRole actorRole,
        ActionStatus status,
        ActionRiskLevel risk,
        boolean approvalRequired,
        UUID approvalId,
        Map<String, String> parameters,
        Map<String, Object> beforeState,
        Map<String, Object> afterState,
        List<ActionDiff> diff,
        String message,
        Instant createdAt,
        Instant executedAt
) {
}
