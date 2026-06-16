package com.opspilot.ai.domain;

public record RecommendedAction(
        String action,
        String risk,
        String reason
) {
}
