package com.opspilot.trends.adapter.out.jpa;

import com.opspilot.trends.application.port.out.KubernetesWorkloadUsageSnapshotStore;
import com.opspilot.trends.domain.KubernetesWorkloadUsageSnapshot;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JpaKubernetesWorkloadUsageSnapshotStore implements KubernetesWorkloadUsageSnapshotStore {

    private final JpaKubernetesWorkloadUsageSnapshotRepository repository;

    @Override
    public List<KubernetesWorkloadUsageSnapshot> saveAll(List<KubernetesWorkloadUsageSnapshot> snapshots) {
        return repository.saveAll(snapshots.stream()
                        .map(KubernetesWorkloadUsageSnapshotEntity::fromDomain)
                        .toList())
                .stream()
                .map(KubernetesWorkloadUsageSnapshotEntity::toDomain)
                .toList();
    }

    @Override
    public List<KubernetesWorkloadUsageSnapshot> find(
            String clusterId,
            String namespace,
            String workloadName,
            Instant from,
            Instant to
    ) {
        return repository.findSnapshots(clusterId, namespace, workloadName, from, to)
                .stream()
                .map(KubernetesWorkloadUsageSnapshotEntity::toDomain)
                .toList();
    }
}
