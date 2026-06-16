package com.opspilot.trends.adapter.out.jpa;

import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.trends.domain.CostDailySnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cost_daily_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CostDailySnapshotEntity {

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

    @Column(nullable = false)
    private String currency;

    @Column(name = "estimated_daily_cost", nullable = false)
    private double estimatedDailyCost;

    @Column(name = "estimated_monthly_cost", nullable = false)
    private double estimatedMonthlyCost;

    @Column(name = "cpu_monthly_cost", nullable = false)
    private double cpuMonthlyCost;

    @Column(name = "memory_monthly_cost", nullable = false)
    private double memoryMonthlyCost;

    @Column(name = "estimated_monthly_saving", nullable = false)
    private double estimatedMonthlySaving;

    @Column(name = "estimation_mode", nullable = false)
    private String estimationMode;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    private CostDailySnapshotEntity(CostDailySnapshot snapshot) {
        this.id = snapshot.id();
        update(snapshot);
    }

    static CostDailySnapshotEntity fromDomain(CostDailySnapshot snapshot) {
        return new CostDailySnapshotEntity(snapshot);
    }

    void update(CostDailySnapshot snapshot) {
        this.clusterId = snapshot.clusterId();
        this.namespace = snapshot.namespace();
        this.workloadKind = snapshot.workloadKind();
        this.workloadName = snapshot.workloadName();
        this.status = snapshot.status().name();
        this.currency = snapshot.currency();
        this.estimatedDailyCost = snapshot.estimatedDailyCost();
        this.estimatedMonthlyCost = snapshot.estimatedMonthlyCost();
        this.cpuMonthlyCost = snapshot.cpuMonthlyCost();
        this.memoryMonthlyCost = snapshot.memoryMonthlyCost();
        this.estimatedMonthlySaving = snapshot.estimatedMonthlySaving();
        this.estimationMode = snapshot.estimationMode();
        this.snapshotDate = snapshot.snapshotDate();
        this.collectedAt = snapshot.collectedAt();
    }

    CostDailySnapshot toDomain() {
        return new CostDailySnapshot(
                id,
                clusterId,
                namespace,
                workloadKind,
                workloadName,
                ResourceStatus.valueOf(status),
                currency,
                estimatedDailyCost,
                estimatedMonthlyCost,
                cpuMonthlyCost,
                memoryMonthlyCost,
                estimatedMonthlySaving,
                estimationMode,
                snapshotDate,
                collectedAt
        );
    }
}
