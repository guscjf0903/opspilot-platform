package com.opspilot.cost.application;

import com.opspilot.cost.config.CostEstimationProperties;
import com.opspilot.cost.application.port.out.OpenCostAllocationPort;
import com.opspilot.cost.domain.CostRecommendation;
import com.opspilot.cost.domain.CostRecommendationType;
import com.opspilot.cost.domain.CostRiskLevel;
import com.opspilot.cost.domain.CostSummary;
import com.opspilot.cost.domain.NamespaceCost;
import com.opspilot.cost.domain.OpenCostAllocationSnapshot;
import com.opspilot.cost.domain.OpenCostWorkloadAllocation;
import com.opspilot.cost.domain.WorkloadCost;
import com.opspilot.kubernetes.application.KubernetesInventoryService;
import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.NamespaceSummary;
import com.opspilot.metrics.application.MetricsQueryService;
import com.opspilot.metrics.domain.MetricsAvailabilityStatus;
import com.opspilot.metrics.domain.ResourceMetricSummary;
import com.opspilot.metrics.domain.ResourceMetrics;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CostQueryService {

    private static final int COST_RANGE_MINUTES = 30;
    private static final double BYTES_PER_GIB = 1024.0 * 1024.0 * 1024.0;
    private static final double CPU_RIGHTSIZING_THRESHOLD_PERCENT = 20.0;
    private static final double MEMORY_RIGHTSIZING_THRESHOLD_PERCENT = 40.0;
    private static final double IDLE_CPU_THRESHOLD_PERCENT = 2.0;
    private static final double IDLE_MEMORY_THRESHOLD_PERCENT = 30.0;
    private static final double MIN_CPU_REQUEST_CORES = 0.05;
    private static final long MIN_MEMORY_REQUEST_BYTES = 64L * 1024 * 1024;

    private final KubernetesInventoryService kubernetesInventoryService;
    private final MetricsQueryService metricsQueryService;
    private final OpenCostAllocationPort openCostAllocationPort;
    private final CostEstimationProperties costEstimationProperties;

    public CostSummary getSummary(String clusterId, String namespace) {
        CostAnalysisSnapshot snapshot = analyze(clusterId, namespace);

        return new CostSummary(
                clusterId,
                costEstimationProperties.getCurrency(),
                roundMoney(snapshot.workloads().stream().mapToDouble(WorkloadCost::estimatedMonthlyCost).sum()),
                estimatedSaving(snapshot.workloads(), snapshot.recommendations()),
                snapshot.namespaces().size(),
                snapshot.workloads().size(),
                snapshot.recommendations().size(),
                snapshot.estimationMode(),
                Instant.now()
        );
    }

    public List<NamespaceCost> getNamespaceCosts(String clusterId, String namespace) {
        return analyze(clusterId, namespace).namespaces();
    }

    public List<WorkloadCost> getWorkloadCosts(String clusterId, String namespace) {
        return analyze(clusterId, namespace).workloads();
    }

    public List<CostRecommendation> getRecommendations(String clusterId, String namespace) {
        return analyze(clusterId, namespace).recommendations();
    }

    public CostAnalysisSnapshot analyze(String clusterId, String namespace) {
        List<NamespaceSummary> namespaces = targetNamespaces(clusterId, namespace);
        OpenCostAllocationSnapshot openCostAllocations = openCostAllocationPort.getWorkloadAllocations(clusterId);
        List<WorkloadCost> workloads = namespaces.stream()
                .flatMap(namespaceSummary -> kubernetesInventoryService
                        .getDeployments(clusterId, namespaceSummary.name())
                        .stream()
                        .map(deployment -> toWorkloadCost(clusterId, deployment, openCostAllocations)))
                .sorted(Comparator
                        .comparing(WorkloadCost::estimatedMonthlyCost)
                        .reversed()
                        .thenComparing(WorkloadCost::namespace)
                        .thenComparing(WorkloadCost::name))
                .toList();
        List<CostRecommendation> recommendations = workloads.stream()
                .flatMap(workload -> recommendationsFor(workload).stream())
                .sorted(Comparator
                        .comparing(CostRecommendation::estimatedMonthlySaving)
                        .reversed()
                        .thenComparing(CostRecommendation::namespace)
                        .thenComparing(CostRecommendation::targetName))
                .toList();
        List<NamespaceCost> namespaceCosts = namespaceCosts(workloads, recommendations);

        return new CostAnalysisSnapshot(
                namespaceCosts,
                workloads,
                recommendations,
                estimationMode(workloads, openCostAllocations)
        );
    }

    private List<NamespaceSummary> targetNamespaces(String clusterId, String namespace) {
        String normalizedNamespace = normalizeNamespace(namespace);
        List<NamespaceSummary> namespaces = kubernetesInventoryService.getNamespaces(clusterId);

        if (normalizedNamespace == null) {
            return namespaces;
        }

        return namespaces.stream()
                .filter(namespaceSummary -> namespaceSummary.name().equals(normalizedNamespace))
                .toList();
    }

    private WorkloadCost toWorkloadCost(
            String clusterId,
            DeploymentSummary deployment,
            OpenCostAllocationSnapshot openCostAllocations
    ) {
        ResourceMetrics metrics = metricsQueryService.getWorkloadMetrics(
                clusterId,
                deployment.namespace(),
                "deployment",
                deployment.name(),
                COST_RANGE_MINUTES
        );
        ResourceMetricSummary summary = metrics.summary();
        Double cpuRequest = summary.cpuRequestCores();
        Long memoryRequest = summary.memoryRequestBytes();
        OpenCostWorkloadAllocation openCostAllocation = openCostAllocations
                .findWorkload(deployment.namespace(), deployment.name())
                .orElse(null);
        double cpuMonthlyCost = cpuMonthlyCost(cpuRequest, openCostAllocation);
        double memoryMonthlyCost = memoryMonthlyCost(memoryRequest, openCostAllocation);
        double estimatedMonthlyCost = estimatedMonthlyCost(cpuMonthlyCost, memoryMonthlyCost, openCostAllocation);

        return new WorkloadCost(
                deployment.namespace(),
                deployment.kind(),
                deployment.name(),
                deployment.status(),
                costEstimationProperties.getCurrency(),
                roundMoney(estimatedMonthlyCost),
                roundMoney(cpuMonthlyCost),
                roundMoney(memoryMonthlyCost),
                cpuRequest,
                memoryRequest,
                summary.cpuUsageCores(),
                summary.memoryUsageBytes(),
                summary.cpuUsagePercent(),
                summary.memoryUsagePercent(),
                metrics.status() == MetricsAvailabilityStatus.AVAILABLE,
                metrics.reason()
        );
    }

    private List<CostRecommendation> recommendationsFor(WorkloadCost workload) {
        if (!workload.metricsAvailable() || workload.estimatedMonthlyCost() <= 0.0) {
            return List.of();
        }

        List<CostRecommendation> recommendations = new ArrayList<>();
        addCpuRightsizingRecommendation(workload, recommendations);
        addMemoryRightsizingRecommendation(workload, recommendations);
        addIdleWorkloadRecommendation(workload, recommendations);

        return recommendations;
    }

    private void addCpuRightsizingRecommendation(
            WorkloadCost workload,
            List<CostRecommendation> recommendations
    ) {
        if (workload.cpuRequestCores() == null
                || workload.cpuUsageCores() == null
                || workload.cpuUsagePercent() == null
                || workload.cpuUsagePercent() > CPU_RIGHTSIZING_THRESHOLD_PERCENT) {
            return;
        }

        double recommendedCpu = Math.max(MIN_CPU_REQUEST_CORES, workload.cpuUsageCores() * 2.0);
        if (recommendedCpu >= workload.cpuRequestCores() * 0.85) {
            return;
        }

        double saving = proportionalSaving(workload.cpuMonthlyCost(), workload.cpuRequestCores(), recommendedCpu);
        if (saving <= 0.0) {
            return;
        }

        recommendations.add(new CostRecommendation(
                recommendationId(workload, CostRecommendationType.CPU_RIGHTSIZING),
                CostRecommendationType.CPU_RIGHTSIZING,
                workload.namespace(),
                workload.kind(),
                workload.name(),
                "%s CPU request 조정 후보".formatted(workload.name()),
                Map.of(
                        "cpuRequest", formatCpu(workload.cpuRequestCores()),
                        "cpuUsage", formatCpu(workload.cpuUsageCores()),
                        "usageRatio", formatPercent(workload.cpuUsagePercent())
                ),
                Map.of("cpuRequest", formatCpu(recommendedCpu)),
                workload.currency(),
                roundMoney(saving),
                riskFor(workload),
                0.78,
                "CPU 사용량이 request 대비 낮아 CPU request를 줄일 여지가 있습니다."
        ));
    }

    private void addMemoryRightsizingRecommendation(
            WorkloadCost workload,
            List<CostRecommendation> recommendations
    ) {
        if (workload.memoryRequestBytes() == null
                || workload.memoryUsageBytes() == null
                || workload.memoryUsagePercent() == null
                || workload.memoryUsagePercent() > MEMORY_RIGHTSIZING_THRESHOLD_PERCENT) {
            return;
        }

        long recommendedMemory = Math.max(MIN_MEMORY_REQUEST_BYTES, Math.round(workload.memoryUsageBytes() * 1.5));
        if (recommendedMemory >= workload.memoryRequestBytes() * 0.85) {
            return;
        }

        double saving = proportionalSaving(
                workload.memoryMonthlyCost(),
                workload.memoryRequestBytes(),
                recommendedMemory
        );
        if (saving <= 0.0) {
            return;
        }

        recommendations.add(new CostRecommendation(
                recommendationId(workload, CostRecommendationType.MEMORY_RIGHTSIZING),
                CostRecommendationType.MEMORY_RIGHTSIZING,
                workload.namespace(),
                workload.kind(),
                workload.name(),
                "%s Memory request 조정 후보".formatted(workload.name()),
                Map.of(
                        "memoryRequest", formatBytes(workload.memoryRequestBytes()),
                        "memoryUsage", formatBytes(workload.memoryUsageBytes()),
                        "usageRatio", formatPercent(workload.memoryUsagePercent())
                ),
                Map.of("memoryRequest", formatBytes(recommendedMemory)),
                workload.currency(),
                roundMoney(saving),
                riskFor(workload),
                0.72,
                "Memory 사용량이 request 대비 낮습니다. OOM 이력 확인 후 보수적으로 축소를 검토하세요."
        ));
    }

    private void addIdleWorkloadRecommendation(
            WorkloadCost workload,
            List<CostRecommendation> recommendations
    ) {
        if (workload.cpuUsagePercent() == null
                || workload.memoryUsagePercent() == null
                || workload.cpuUsagePercent() > IDLE_CPU_THRESHOLD_PERCENT
                || workload.memoryUsagePercent() > IDLE_MEMORY_THRESHOLD_PERCENT) {
            return;
        }

        recommendations.add(new CostRecommendation(
                recommendationId(workload, CostRecommendationType.IDLE_WORKLOAD),
                CostRecommendationType.IDLE_WORKLOAD,
                workload.namespace(),
                workload.kind(),
                workload.name(),
                "%s 유휴 워크로드 후보".formatted(workload.name()),
                Map.of(
                        "estimatedMonthlyCost", formatMoney(workload.estimatedMonthlyCost(), workload.currency()),
                        "cpuUsageRatio", formatPercent(workload.cpuUsagePercent()),
                        "memoryUsageRatio", formatPercent(workload.memoryUsagePercent())
                ),
                Map.of("action", "dev/staging 환경이면 scale-down 또는 scheduled scaling 검토"),
                workload.currency(),
                roundMoney(workload.estimatedMonthlyCost() * 0.5),
                riskFor(workload),
                0.66,
                "최근 metric 기준으로 CPU와 Memory 사용률이 모두 낮습니다."
        ));
    }

    private List<NamespaceCost> namespaceCosts(
            List<WorkloadCost> workloads,
            List<CostRecommendation> recommendations
    ) {
        Map<String, List<WorkloadCost>> workloadsByNamespace = new LinkedHashMap<>();
        workloads.forEach(workload -> workloadsByNamespace
                .computeIfAbsent(workload.namespace(), ignored -> new ArrayList<>())
                .add(workload));

        return workloadsByNamespace.entrySet().stream()
                .map(entry -> {
                    String namespace = entry.getKey();
                    double monthlyCost = entry.getValue().stream()
                            .mapToDouble(WorkloadCost::estimatedMonthlyCost)
                            .sum();
                    double monthlySaving = estimatedSaving(entry.getValue(), recommendations);

                    return new NamespaceCost(
                            namespace,
                            costEstimationProperties.getCurrency(),
                            roundMoney(monthlyCost),
                            roundMoney(monthlySaving),
                            entry.getValue().size(),
                            (int) recommendations.stream()
                                    .filter(recommendation -> recommendation.namespace().equals(namespace))
                                    .count()
                    );
                })
                .sorted(Comparator
                        .comparing(NamespaceCost::estimatedMonthlyCost)
                        .reversed()
                        .thenComparing(NamespaceCost::namespace))
                .toList();
    }

    private CostRiskLevel riskFor(WorkloadCost workload) {
        String namespace = workload.namespace().toLowerCase(Locale.ROOT);

        if (namespace.equals("prod") || namespace.equals("production")) {
            return CostRiskLevel.MEDIUM;
        }

        if (namespace.startsWith("kube-") || namespace.equals("monitoring")) {
            return CostRiskLevel.MEDIUM;
        }

        return CostRiskLevel.LOW;
    }

    private String recommendationId(WorkloadCost workload, CostRecommendationType type) {
        return "%s:%s:%s:%s".formatted(type.name(), workload.namespace(), workload.kind(), workload.name());
    }

    private double costForCpu(Double cpuRequestCores) {
        if (cpuRequestCores == null || cpuRequestCores <= 0.0) {
            return 0.0;
        }

        return cpuRequestCores * costEstimationProperties.getCpuCoreMonthlyPrice();
    }

    private double costForMemory(Long memoryRequestBytes) {
        if (memoryRequestBytes == null || memoryRequestBytes <= 0L) {
            return 0.0;
        }

        return memoryRequestBytes / BYTES_PER_GIB * costEstimationProperties.getMemoryGibMonthlyPrice();
    }

    private double cpuMonthlyCost(Double cpuRequest, OpenCostWorkloadAllocation openCostAllocation) {
        if (openCostAllocation != null && openCostAllocation.cpuMonthlyCost() > 0.0) {
            return openCostAllocation.cpuMonthlyCost();
        }

        return costForCpu(cpuRequest);
    }

    private double memoryMonthlyCost(Long memoryRequest, OpenCostWorkloadAllocation openCostAllocation) {
        if (openCostAllocation != null && openCostAllocation.memoryMonthlyCost() > 0.0) {
            return openCostAllocation.memoryMonthlyCost();
        }

        return costForMemory(memoryRequest);
    }

    private double estimatedMonthlyCost(
            double cpuMonthlyCost,
            double memoryMonthlyCost,
            OpenCostWorkloadAllocation openCostAllocation
    ) {
        if (openCostAllocation != null && openCostAllocation.monthlyCost() > 0.0) {
            return openCostAllocation.monthlyCost();
        }

        return cpuMonthlyCost + memoryMonthlyCost;
    }

    private double proportionalSaving(double currentMonthlyCost, double currentRequest, double recommendedRequest) {
        if (currentMonthlyCost <= 0.0 || currentRequest <= 0.0 || recommendedRequest >= currentRequest) {
            return 0.0;
        }

        return roundMoney(currentMonthlyCost * ((currentRequest - recommendedRequest) / currentRequest));
    }

    private double estimatedSaving(
            List<WorkloadCost> workloads,
            List<CostRecommendation> recommendations
    ) {
        double saving = workloads.stream()
                .mapToDouble(workload -> cappedWorkloadSaving(workload, recommendations))
                .sum();

        return roundMoney(saving);
    }

    private double cappedWorkloadSaving(
            WorkloadCost workload,
            List<CostRecommendation> recommendations
    ) {
        double candidateSaving = recommendations.stream()
                .filter(recommendation -> recommendation.namespace().equals(workload.namespace()))
                .filter(recommendation -> recommendation.targetKind().equals(workload.kind()))
                .filter(recommendation -> recommendation.targetName().equals(workload.name()))
                .mapToDouble(CostRecommendation::estimatedMonthlySaving)
                .sum();

        return Math.min(workload.estimatedMonthlyCost(), candidateSaving);
    }

    private String estimationMode(
            List<WorkloadCost> workloads,
            OpenCostAllocationSnapshot openCostAllocations
    ) {
        if (!openCostAllocations.available() || workloads.isEmpty()) {
            return "REQUEST_BASED_LOCAL_ESTIMATE";
        }

        long matchedCount = workloads.stream()
                .filter(workload -> openCostAllocations.findWorkload(workload.namespace(), workload.name()).isPresent())
                .count();

        if (matchedCount == workloads.size()) {
            return "OPENCOST_ALLOCATION";
        }

        if (matchedCount > 0) {
            return "OPENCOST_ALLOCATION_WITH_REQUEST_FALLBACK";
        }

        return "REQUEST_BASED_LOCAL_ESTIMATE";
    }

    private String normalizeNamespace(String namespace) {
        return namespace == null || namespace.isBlank() ? null : namespace.trim();
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String formatMoney(double value, String currency) {
        return "%s %.2f".formatted(currency, value);
    }

    private String formatCpu(Double cores) {
        if (cores == null) {
            return "-";
        }

        return "%.0fm".formatted(cores * 1000.0);
    }

    private String formatBytes(Long bytes) {
        if (bytes == null) {
            return "-";
        }

        double mib = bytes / 1024.0 / 1024.0;

        if (mib >= 1024.0) {
            return "%.1fGi".formatted(mib / 1024.0);
        }

        return "%.0fMi".formatted(mib);
    }

    private String formatPercent(Double percent) {
        if (percent == null) {
            return "-";
        }

        return "%.1f%%".formatted(percent);
    }
}
