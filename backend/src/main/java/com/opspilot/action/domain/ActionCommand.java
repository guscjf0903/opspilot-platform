package com.opspilot.action.domain;

import java.util.Map;

public record ActionCommand(
        String clusterId,
        ActionType type,
        String namespace,
        String targetKind,
        String targetName,
        Map<String, String> parameters,
        String reason
) {
}
