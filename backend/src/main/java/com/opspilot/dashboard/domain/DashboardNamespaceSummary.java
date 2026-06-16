package com.opspilot.dashboard.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;

public record DashboardNamespaceSummary(
        String name,
        ResourceStatus status,
        int deploymentCount,
        int podCount,
        int unhealthyWorkloadCount
) {
}
