package com.opspilot.dashboard.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;

public record DashboardWorkloadSummary(
        String kind,
        String namespace,
        String name,
        ResourceStatus status,
        String reason,
        String message
) {
}
