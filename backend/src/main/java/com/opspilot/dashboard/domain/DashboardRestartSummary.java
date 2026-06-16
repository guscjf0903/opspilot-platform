package com.opspilot.dashboard.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;

public record DashboardRestartSummary(
        String namespace,
        String name,
        ResourceStatus status,
        String reason,
        int restartCount
) {
}
