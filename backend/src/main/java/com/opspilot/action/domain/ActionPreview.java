package com.opspilot.action.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ActionPreview(
        String clusterId,
        ActionType type,
        String namespace,
        String targetKind,
        String targetName,
        ActionRiskLevel risk,
        boolean approvalRequired,
        boolean executable,
        Map<String, Object> beforeState,
        Map<String, Object> afterState,
        List<ActionDiff> diff,
        List<String> warnings,
        String message,
        Instant createdAt
) {
}
