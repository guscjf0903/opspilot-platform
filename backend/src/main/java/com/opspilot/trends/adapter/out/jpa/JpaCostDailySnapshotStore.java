package com.opspilot.trends.adapter.out.jpa;

import com.opspilot.trends.application.port.out.CostDailySnapshotStore;
import com.opspilot.trends.domain.CostDailySnapshot;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JpaCostDailySnapshotStore implements CostDailySnapshotStore {

    private final JpaCostDailySnapshotRepository repository;

    @Override
    public List<CostDailySnapshot> upsertAll(List<CostDailySnapshot> snapshots) {
        return snapshots.stream()
                .map(this::upsert)
                .toList();
    }

    @Override
    public List<CostDailySnapshot> find(
            String clusterId,
            String namespace,
            String workloadName,
            LocalDate from,
            LocalDate to
    ) {
        return repository.findSnapshots(clusterId, namespace, workloadName, from, to)
                .stream()
                .map(CostDailySnapshotEntity::toDomain)
                .toList();
    }

    private CostDailySnapshot upsert(CostDailySnapshot snapshot) {
        CostDailySnapshotEntity entity = repository
                .findByClusterIdAndNamespaceAndWorkloadKindAndWorkloadNameAndSnapshotDate(
                        snapshot.clusterId(),
                        snapshot.namespace(),
                        snapshot.workloadKind(),
                        snapshot.workloadName(),
                        snapshot.snapshotDate()
                )
                .orElseGet(() -> CostDailySnapshotEntity.fromDomain(snapshot));
        entity.update(snapshot);

        return repository.save(entity).toDomain();
    }
}
