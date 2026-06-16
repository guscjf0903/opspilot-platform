package com.opspilot.trends.domain;

import com.opspilot.kubernetes.domain.ResourceStatus;
import java.time.Instant;
import java.util.UUID;

public record KubernetesWorkloadUsageSnapshot(
        UUID id,
        String clusterId,
        String namespace,
        String workloadKind,
        String workloadName,
        ResourceStatus status,
        int desiredReplicas,
        int availableReplicas,
        int readyReplicas,
        Double cpuRequestCores,
        Long memoryRequestBytes,
        Double cpuUsageAvgCores,
        Double cpuUsageP95Cores,
        Long memoryUsageAvgBytes,
        Long memoryUsageP95Bytes,
        Double cpuUsagePercent,
        Double memoryUsagePercent,
        boolean metricsAvailable,
        String metricsReason,
        int rangeMinutes,
        Instant collectedAt
) {
}
