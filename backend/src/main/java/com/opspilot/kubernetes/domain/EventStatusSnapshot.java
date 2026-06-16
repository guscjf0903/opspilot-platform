package com.opspilot.kubernetes.domain;

public record EventStatusSnapshot(
        String type,
        String reason,
        String message
) {
}
