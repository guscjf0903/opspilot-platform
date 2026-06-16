package com.opspilot.dashboard.domain;

public record DashboardCounts(
        int namespaceCount,
        int nodeCount,
        int deploymentCount,
        int podCount,
        int criticalWorkloadCount,
        int warningWorkloadCount,
        int recentWarningEventCount
) {
}
