package com.opspilot.trends.application.port.out;

import com.opspilot.trends.domain.KubernetesWorkloadUsageSnapshot;
import java.time.Instant;
import java.util.List;

public interface KubernetesWorkloadUsageSnapshotStore {

    List<KubernetesWorkloadUsageSnapshot> saveAll(List<KubernetesWorkloadUsageSnapshot> snapshots);

    List<KubernetesWorkloadUsageSnapshot> find(
            String clusterId,
            String namespace,
            String workloadName,
            Instant from,
            Instant to
    );
}
