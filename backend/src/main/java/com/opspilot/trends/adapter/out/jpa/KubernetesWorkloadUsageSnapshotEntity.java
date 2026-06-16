package com.opspilot.trends.adapter.out.jpa;

import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.trends.domain.KubernetesWorkloadUsageSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kubernetes_workload_usage_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class KubernetesWorkloadUsageSnapshotEntity {

    @Id
    private UUID id;

    @Column(name = "cluster_id", nullable = false)
    private String clusterId;

    @Column(nullable = false)
    private String namespace;

    @Column(name = "workload_kind", nullable = false)
    private String workloadKind;

    @Column(name = "workload_name", nullable = false)
    private String workloadName;

    @Column(nullable = false)
    private String status;

    @Column(name = "desired_replicas", nullable = false)
    private int desiredReplicas;

    @Column(name = "available_replicas", nullable = false)
    private int availableReplicas;

    @Column(name = "ready_replicas", nullable = false)
    private int readyReplicas;

    @Column(name = "cpu_request_cores")
    private Double cpuRequestCores;

    @Column(name = "memory_request_bytes")
    private Long memoryRequestBytes;

    @Column(name = "cpu_usage_avg_cores")
    private Double cpuUsageAvgCores;

    @Column(name = "cpu_usage_p95_cores")
    private Double cpuUsageP95Cores;

    @Column(name = "memory_usage_avg_bytes")
    private Long memoryUsageAvgBytes;

    @Column(name = "memory_usage_p95_bytes")
    private Long memoryUsageP95Bytes;

    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;

    @Column(name = "memory_usage_percent")
    private Double memoryUsagePercent;

    @Column(name = "metrics_available", nullable = false)
    private boolean metricsAvailable;

    @Column(name = "metrics_reason", nullable = false)
    private String metricsReason;

    @Column(name = "range_minutes", nullable = false)
    private int rangeMinutes;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    private KubernetesWorkloadUsageSnapshotEntity(KubernetesWorkloadUsageSnapshot snapshot) {
        this.id = snapshot.id();
        this.clusterId = snapshot.clusterId();
        this.namespace = snapshot.namespace();
        this.workloadKind = snapshot.workloadKind();
        this.workloadName = snapshot.workloadName();
        this.status = snapshot.status().name();
        this.desiredReplicas = snapshot.desiredReplicas();
        this.availableReplicas = snapshot.availableReplicas();
        this.readyReplicas = snapshot.readyReplicas();
        this.cpuRequestCores = snapshot.cpuRequestCores();
        this.memoryRequestBytes = snapshot.memoryRequestBytes();
        this.cpuUsageAvgCores = snapshot.cpuUsageAvgCores();
        this.cpuUsageP95Cores = snapshot.cpuUsageP95Cores();
        this.memoryUsageAvgBytes = snapshot.memoryUsageAvgBytes();
        this.memoryUsageP95Bytes = snapshot.memoryUsageP95Bytes();
        this.cpuUsagePercent = snapshot.cpuUsagePercent();
        this.memoryUsagePercent = snapshot.memoryUsagePercent();
        this.metricsAvailable = snapshot.metricsAvailable();
        this.metricsReason = snapshot.metricsReason();
        this.rangeMinutes = snapshot.rangeMinutes();
        this.collectedAt = snapshot.collectedAt();
    }

    static KubernetesWorkloadUsageSnapshotEntity fromDomain(KubernetesWorkloadUsageSnapshot snapshot) {
        return new KubernetesWorkloadUsageSnapshotEntity(snapshot);
    }

    KubernetesWorkloadUsageSnapshot toDomain() {
        return new KubernetesWorkloadUsageSnapshot(
                id,
                clusterId,
                namespace,
                workloadKind,
                workloadName,
                ResourceStatus.valueOf(status),
                desiredReplicas,
                availableReplicas,
                readyReplicas,
                cpuRequestCores,
                memoryRequestBytes,
                cpuUsageAvgCores,
                cpuUsageP95Cores,
                memoryUsageAvgBytes,
                memoryUsageP95Bytes,
                cpuUsagePercent,
                memoryUsagePercent,
                metricsAvailable,
                metricsReason,
                rangeMinutes,
                collectedAt
        );
    }
}
