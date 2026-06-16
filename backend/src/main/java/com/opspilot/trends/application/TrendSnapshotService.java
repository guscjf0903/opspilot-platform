package com.opspilot.trends.application;

import com.opspilot.cost.application.CostAnalysisSnapshot;
import com.opspilot.cost.application.CostQueryService;
import com.opspilot.cost.domain.CostRecommendation;
import com.opspilot.cost.domain.WorkloadCost;
import com.opspilot.kubernetes.application.UnknownClusterException;
import com.opspilot.kubernetes.application.KubernetesInventoryService;
import com.opspilot.kubernetes.config.OpspilotKubernetesProperties;
import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.NamespaceSummary;
import com.opspilot.metrics.application.MetricsQueryService;
import com.opspilot.metrics.domain.MetricPoint;
import com.opspilot.metrics.domain.MetricsAvailabilityStatus;
import com.opspilot.metrics.domain.ResourceMetricSummary;
import com.opspilot.metrics.domain.ResourceMetrics;
import com.opspilot.trends.application.port.out.CostDailySnapshotStore;
import com.opspilot.trends.application.port.out.KubernetesWorkloadUsageSnapshotStore;
import com.opspilot.trends.domain.CostDailySnapshot;
import com.opspilot.trends.domain.KubernetesWorkloadUsageSnapshot;
import com.opspilot.trends.domain.TrendSnapshotCollectionResult;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrendSnapshotService {

    private static final int WORKLOAD_USAGE_RANGE_MINUTES = 60;
    private static final double DAYS_PER_MONTH = 30.0;

    private final KubernetesInventoryService kubernetesInventoryService;
    private final MetricsQueryService metricsQueryService;
    private final CostQueryService costQueryService;
    private final KubernetesWorkloadUsageSnapshotStore kubernetesSnapshotStore;
    private final CostDailySnapshotStore costDailySnapshotStore;
    private final OpspilotKubernetesProperties kubernetesProperties;

    @Transactional
    public TrendSnapshotCollectionResult collect(String clusterId, String namespace) {
        validateCluster(clusterId);
        Instant collectedAt = Instant.now();
        List<KubernetesWorkloadUsageSnapshot> kubernetesSnapshots =
                collectKubernetesWorkloadUsage(clusterId, normalizeNamespace(namespace), collectedAt);
        CostAnalysisSnapshot costAnalysis = costQueryService.analyze(clusterId, namespace);
        List<CostDailySnapshot> costSnapshots = collectCostDailySnapshots(clusterId, costAnalysis, collectedAt);

        kubernetesSnapshotStore.saveAll(kubernetesSnapshots);
        costDailySnapshotStore.upsertAll(costSnapshots);

        return new TrendSnapshotCollectionResult(
                clusterId,
                kubernetesSnapshots.size(),
                costSnapshots.size(),
                collectedAt
        );
    }

    @Transactional(readOnly = true)
    public List<KubernetesWorkloadUsageSnapshot> getKubernetesWorkloadUsageSnapshots(
            String clusterId,
            String namespace,
            String workloadName,
            Instant from,
            Instant to
    ) {
        validateCluster(clusterId);
        Instant resolvedTo = to == null ? Instant.now() : to;
        Instant resolvedFrom = from == null ? resolvedTo.minusSeconds(30L * 24L * 60L * 60L) : from;

        return kubernetesSnapshotStore.find(
                clusterId,
                normalizeNamespace(namespace),
                normalizeWorkloadName(workloadName),
                resolvedFrom,
                resolvedTo
        );
    }

    @Transactional(readOnly = true)
    public List<CostDailySnapshot> getCostDailySnapshots(
            String clusterId,
            String namespace,
            String workloadName,
            LocalDate from,
            LocalDate to
    ) {
        validateCluster(clusterId);
        LocalDate resolvedTo = to == null ? LocalDate.now(ZoneId.systemDefault()) : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(90) : from;

        return costDailySnapshotStore.find(
                clusterId,
                normalizeNamespace(namespace),
                normalizeWorkloadName(workloadName),
                resolvedFrom,
                resolvedTo
        );
    }

    private List<KubernetesWorkloadUsageSnapshot> collectKubernetesWorkloadUsage(
            String clusterId,
            String namespace,
            Instant collectedAt
    ) {
        return targetNamespaces(clusterId, namespace).stream()
                .flatMap(namespaceSummary -> kubernetesInventoryService
                        .getDeployments(clusterId, namespaceSummary.name())
                        .stream()
                        .map(deployment -> toKubernetesSnapshot(clusterId, deployment, collectedAt)))
                .toList();
    }

    private KubernetesWorkloadUsageSnapshot toKubernetesSnapshot(
            String clusterId,
            DeploymentSummary deployment,
            Instant collectedAt
    ) {
        ResourceMetrics metrics = metricsQueryService.getWorkloadMetrics(
                clusterId,
                deployment.namespace(),
                "deployment",
                deployment.name(),
                WORKLOAD_USAGE_RANGE_MINUTES
        );
        ResourceMetricSummary summary = metrics.summary();

        return new KubernetesWorkloadUsageSnapshot(
                UUID.randomUUID(),
                clusterId,
                deployment.namespace(),
                deployment.kind(),
                deployment.name(),
                deployment.status(),
                deployment.desiredReplicas(),
                deployment.availableReplicas(),
                deployment.readyReplicas(),
                summary.cpuRequestCores(),
                summary.memoryRequestBytes(),
                average(metrics.cpu().points()),
                percentile(metrics.cpu().points(), 0.95),
                averageLong(metrics.memory().points()),
                percentileLong(metrics.memory().points(), 0.95),
                summary.cpuUsagePercent(),
                summary.memoryUsagePercent(),
                metrics.status() == MetricsAvailabilityStatus.AVAILABLE,
                metrics.reason(),
                metrics.rangeMinutes(),
                collectedAt
        );
    }

    private List<CostDailySnapshot> collectCostDailySnapshots(
            String clusterId,
            CostAnalysisSnapshot costAnalysis,
            Instant collectedAt
    ) {
        LocalDate snapshotDate = LocalDate.now(ZoneId.systemDefault());
        List<CostRecommendation> recommendations = costAnalysis.recommendations();

        return costAnalysis.workloads().stream()
                .map(workload -> toCostDailySnapshot(
                        clusterId,
                        workload,
                        recommendations,
                        costAnalysis.estimationMode(),
                        snapshotDate,
                        collectedAt
                ))
                .toList();
    }

    private CostDailySnapshot toCostDailySnapshot(
            String clusterId,
            WorkloadCost workload,
            List<CostRecommendation> recommendations,
            String estimationMode,
            LocalDate snapshotDate,
            Instant collectedAt
    ) {
        double monthlySaving = estimatedMonthlySaving(workload, recommendations);

        return new CostDailySnapshot(
                UUID.randomUUID(),
                clusterId,
                workload.namespace(),
                workload.kind(),
                workload.name(),
                workload.status(),
                workload.currency(),
                roundMoney(workload.estimatedMonthlyCost() / DAYS_PER_MONTH),
                workload.estimatedMonthlyCost(),
                workload.cpuMonthlyCost(),
                workload.memoryMonthlyCost(),
                monthlySaving,
                estimationMode,
                snapshotDate,
                collectedAt
        );
    }

    private List<NamespaceSummary> targetNamespaces(String clusterId, String namespace) {
        List<NamespaceSummary> namespaces = kubernetesInventoryService.getNamespaces(clusterId);

        if (namespace == null) {
            return namespaces;
        }

        return namespaces.stream()
                .filter(namespaceSummary -> namespaceSummary.name().equals(namespace))
                .toList();
    }

    private double estimatedMonthlySaving(WorkloadCost workload, List<CostRecommendation> recommendations) {
        double saving = recommendations.stream()
                .filter(recommendation -> recommendation.namespace().equals(workload.namespace()))
                .filter(recommendation -> recommendation.targetKind().equals(workload.kind()))
                .filter(recommendation -> recommendation.targetName().equals(workload.name()))
                .mapToDouble(CostRecommendation::estimatedMonthlySaving)
                .sum();

        return roundMoney(Math.min(workload.estimatedMonthlyCost(), saving));
    }

    private Double average(List<MetricPoint> points) {
        if (points.isEmpty()) {
            return null;
        }

        return points.stream()
                .mapToDouble(MetricPoint::value)
                .average()
                .orElse(Double.NaN);
    }

    private Long averageLong(List<MetricPoint> points) {
        Double average = average(points);

        return average == null || !Double.isFinite(average) ? null : Math.round(average);
    }

    private Double percentile(List<MetricPoint> points, double percentile) {
        if (points.isEmpty()) {
            return null;
        }

        List<Double> values = points.stream()
                .map(MetricPoint::value)
                .filter(Double::isFinite)
                .sorted()
                .toList();
        if (values.isEmpty()) {
            return null;
        }

        int index = (int) Math.ceil(percentile * values.size()) - 1;

        return values.get(Math.max(0, Math.min(index, values.size() - 1)));
    }

    private Long percentileLong(List<MetricPoint> points, double percentile) {
        Double value = percentile(points, percentile);

        return value == null ? null : Math.round(value);
    }

    private String normalizeNamespace(String namespace) {
        return namespace == null || namespace.isBlank() ? null : namespace.trim();
    }

    private String normalizeWorkloadName(String workloadName) {
        return workloadName == null || workloadName.isBlank() ? null : workloadName.trim();
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void validateCluster(String clusterId) {
        if (!kubernetesProperties.getClusterId().equals(clusterId)) {
            throw new UnknownClusterException(clusterId);
        }
    }
}
