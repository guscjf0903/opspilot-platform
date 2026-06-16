package com.opspilot.dashboard.domain;

import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.kubernetes.domain.NodeSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;
import java.util.List;

public record DashboardSummary(
        ResourceStatus clusterStatus,
        String selectedNamespace,
        Instant collectedAt,
        DashboardCounts counts,
        List<DashboardWorkloadSummary> unhealthyWorkloads,
        List<EventSummary> recentWarningEvents,
        List<DashboardRestartSummary> restartCountTopPods,
        List<DashboardNamespaceSummary> namespaces,
        List<NodeSummary> nodes,
        DashboardExternalSignal nodeUsage,
        DashboardExternalSignal kafkaLag,
        DashboardExternalSignal cost
) {
}
