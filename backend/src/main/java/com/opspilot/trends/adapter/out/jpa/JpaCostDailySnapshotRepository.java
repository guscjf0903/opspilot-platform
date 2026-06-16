package com.opspilot.trends.adapter.out.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaCostDailySnapshotRepository extends JpaRepository<CostDailySnapshotEntity, UUID> {

    Optional<CostDailySnapshotEntity> findByClusterIdAndNamespaceAndWorkloadKindAndWorkloadNameAndSnapshotDate(
            String clusterId,
            String namespace,
            String workloadKind,
            String workloadName,
            LocalDate snapshotDate
    );

    @Query("""
            select snapshot
            from CostDailySnapshotEntity snapshot
            where snapshot.clusterId = :clusterId
              and (:namespace is null or snapshot.namespace = :namespace)
              and (:workloadName is null or snapshot.workloadName = :workloadName)
              and snapshot.snapshotDate between :from and :to
            order by snapshot.snapshotDate asc, snapshot.namespace asc, snapshot.workloadName asc
            """)
    List<CostDailySnapshotEntity> findSnapshots(
            @Param("clusterId") String clusterId,
            @Param("namespace") String namespace,
            @Param("workloadName") String workloadName,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
