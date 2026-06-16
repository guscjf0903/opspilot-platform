package com.opspilot.trends.adapter.out.jpa;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaKubernetesWorkloadUsageSnapshotRepository
        extends JpaRepository<KubernetesWorkloadUsageSnapshotEntity, UUID> {

    @Query("""
            select snapshot
            from KubernetesWorkloadUsageSnapshotEntity snapshot
            where snapshot.clusterId = :clusterId
              and (:namespace is null or snapshot.namespace = :namespace)
              and (:workloadName is null or snapshot.workloadName = :workloadName)
              and snapshot.collectedAt between :from and :to
            order by snapshot.collectedAt asc, snapshot.namespace asc, snapshot.workloadName asc
            """)
    List<KubernetesWorkloadUsageSnapshotEntity> findSnapshots(
            @Param("clusterId") String clusterId,
            @Param("namespace") String namespace,
            @Param("workloadName") String workloadName,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
