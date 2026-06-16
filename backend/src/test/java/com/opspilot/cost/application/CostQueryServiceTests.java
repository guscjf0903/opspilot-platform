package com.opspilot.cost.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.opspilot.cost.config.CostEstimationProperties;
import com.opspilot.cost.application.port.out.OpenCostAllocationPort;
import com.opspilot.cost.domain.CostRecommendationType;
import com.opspilot.cost.domain.OpenCostAllocationSnapshot;
import com.opspilot.cost.domain.OpenCostWorkloadAllocation;
import com.opspilot.kubernetes.application.KubernetesInventoryService;
import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.NamespaceSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.metrics.application.MetricsQueryService;
import com.opspilot.metrics.domain.MetricQueryWindow;
import com.opspilot.metrics.domain.MetricSeries;
import com.opspilot.metrics.domain.ResourceMetricSummary;
import com.opspilot.metrics.domain.ResourceMetrics;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CostQueryServiceTests {

    private static final Instant NOW = Instant.parse("2026-06-04T00:30:00Z");

    private KubernetesInventoryService kubernetesInventoryService;
    private MetricsQueryService metricsQueryService;
    private OpenCostAllocationPort openCostAllocationPort;
    private CostQueryService costQueryService;

    @BeforeEach
    void setUp() {
        kubernetesInventoryService = mock(KubernetesInventoryService.class);
        metricsQueryService = mock(MetricsQueryService.class);
        openCostAllocationPort = mock(OpenCostAllocationPort.class);
        when(openCostAllocationPort.getWorkloadAllocations("local"))
                .thenReturn(OpenCostAllocationSnapshot.unavailable("OPENCOST_DISABLED"));
        CostEstimationProperties properties = new CostEstimationProperties();
        costQueryService = new CostQueryService(
                kubernetesInventoryService,
                metricsQueryService,
                openCostAllocationPort,
                properties
        );
    }

    @Test
    void estimatesWorkloadCostAndCreatesRecommendations() {
        when(kubernetesInventoryService.getNamespaces("local"))
                .thenReturn(List.of(namespace("sample-app")));
        when(kubernetesInventoryService.getDeployments("local", "sample-app"))
                .thenReturn(List.of(deployment("sample-app", "payment-api")));
        when(metricsQueryService.getWorkloadMetrics(
                eq("local"),
                eq("sample-app"),
                eq("deployment"),
                eq("payment-api"),
                anyInt()
        )).thenReturn(metrics(
                1.0,
                1024L * 1024 * 1024,
                0.05,
                128L * 1024 * 1024,
                5.0,
                12.5
        ));

        var summary = costQueryService.getSummary("local", null);
        var workloads = costQueryService.getWorkloadCosts("local", null);
        var recommendations = costQueryService.getRecommendations("local", null);

        assertThat(summary.estimatedMonthlyCost()).isEqualTo(27.0);
        assertThat(summary.estimationMode()).isEqualTo("REQUEST_BASED_LOCAL_ESTIMATE");
        assertThat(workloads).singleElement()
                .satisfies(workload -> {
                    assertThat(workload.cpuRequestCores()).isEqualTo(1.0);
                    assertThat(workload.memoryRequestBytes()).isEqualTo(1024L * 1024 * 1024);
                    assertThat(workload.metricsAvailable()).isTrue();
                });
        assertThat(recommendations).extracting(recommendation -> recommendation.type())
                .contains(
                        CostRecommendationType.CPU_RIGHTSIZING,
                        CostRecommendationType.MEMORY_RIGHTSIZING
                );
    }

    @Test
    void detectsIdleWorkloadWhenCpuAndMemoryUsageAreVeryLow() {
        when(kubernetesInventoryService.getNamespaces("local"))
                .thenReturn(List.of(namespace("dev")));
        when(kubernetesInventoryService.getDeployments("local", "dev"))
                .thenReturn(List.of(deployment("dev", "idle-worker")));
        when(metricsQueryService.getWorkloadMetrics(
                eq("local"),
                eq("dev"),
                eq("deployment"),
                eq("idle-worker"),
                anyInt()
        )).thenReturn(metrics(
                0.5,
                512L * 1024 * 1024,
                0.005,
                32L * 1024 * 1024,
                1.0,
                6.25
        ));

        var recommendations = costQueryService.getRecommendations("local", "dev");

        assertThat(recommendations).extracting(recommendation -> recommendation.type())
                .contains(CostRecommendationType.IDLE_WORKLOAD);
    }

    @Test
    void capsSummarySavingAtWorkloadMonthlyCostWhenRecommendationsOverlap() {
        when(kubernetesInventoryService.getNamespaces("local"))
                .thenReturn(List.of(namespace("dev")));
        when(kubernetesInventoryService.getDeployments("local", "dev"))
                .thenReturn(List.of(deployment("dev", "idle-worker")));
        when(metricsQueryService.getWorkloadMetrics(
                eq("local"),
                eq("dev"),
                eq("deployment"),
                eq("idle-worker"),
                anyInt()
        )).thenReturn(metrics(
                1.0,
                1024L * 1024 * 1024,
                0.005,
                32L * 1024 * 1024,
                1.0,
                6.25
        ));

        var summary = costQueryService.getSummary("local", "dev");
        var namespaces = costQueryService.getNamespaceCosts("local", "dev");
        var recommendations = costQueryService.getRecommendations("local", "dev");

        assertThat(recommendations).extracting(recommendation -> recommendation.type())
                .contains(
                        CostRecommendationType.CPU_RIGHTSIZING,
                        CostRecommendationType.MEMORY_RIGHTSIZING,
                        CostRecommendationType.IDLE_WORKLOAD
                );
        assertThat(summary.estimatedMonthlyCost()).isEqualTo(27.0);
        assertThat(summary.estimatedMonthlySaving()).isLessThanOrEqualTo(summary.estimatedMonthlyCost());
        assertThat(summary.estimatedMonthlySaving()).isEqualTo(27.0);
        assertThat(namespaces).singleElement()
                .satisfies(namespace -> assertThat(namespace.estimatedMonthlySaving())
                        .isLessThanOrEqualTo(namespace.estimatedMonthlyCost()));
    }

    @Test
    void usesOpenCostAllocationWhenDeploymentCostIsAvailable() {
        when(kubernetesInventoryService.getNamespaces("local"))
                .thenReturn(List.of(namespace("sample-app")));
        when(kubernetesInventoryService.getDeployments("local", "sample-app"))
                .thenReturn(List.of(deployment("sample-app", "payment-api")));
        when(openCostAllocationPort.getWorkloadAllocations("local"))
                .thenReturn(OpenCostAllocationSnapshot.available(List.of(
                        new OpenCostWorkloadAllocation("sample-app", "payment-api", 42.5, 30.0, 10.0)
                )));
        when(metricsQueryService.getWorkloadMetrics(
                eq("local"),
                eq("sample-app"),
                eq("deployment"),
                eq("payment-api"),
                anyInt()
        )).thenReturn(metrics(
                1.0,
                1024L * 1024 * 1024,
                0.05,
                128L * 1024 * 1024,
                5.0,
                12.5
        ));

        var summary = costQueryService.getSummary("local", null);
        var workloads = costQueryService.getWorkloadCosts("local", null);
        var recommendations = costQueryService.getRecommendations("local", null);

        assertThat(summary.estimatedMonthlyCost()).isEqualTo(42.5);
        assertThat(summary.estimationMode()).isEqualTo("OPENCOST_ALLOCATION");
        assertThat(workloads).singleElement()
                .satisfies(workload -> {
                    assertThat(workload.estimatedMonthlyCost()).isEqualTo(42.5);
                    assertThat(workload.cpuMonthlyCost()).isEqualTo(30.0);
                    assertThat(workload.memoryMonthlyCost()).isEqualTo(10.0);
                });
        assertThat(recommendations).extracting(recommendation -> recommendation.type())
                .contains(CostRecommendationType.CPU_RIGHTSIZING);
    }

    private NamespaceSummary namespace(String name) {
        return new NamespaceSummary("Namespace", name, ResourceStatus.HEALTHY, "Healthy", "Resource is healthy", NOW);
    }

    private DeploymentSummary deployment(String namespace, String name) {
        return new DeploymentSummary(
                "Deployment",
                namespace,
                name,
                ResourceStatus.HEALTHY,
                "Healthy",
                "Resource is healthy",
                NOW,
                1,
                1,
                1,
                1
        );
    }

    private ResourceMetrics metrics(
            double cpuRequest,
            long memoryRequest,
            double cpuUsage,
            long memoryUsage,
            double cpuPercent,
            double memoryPercent
    ) {
        MetricQueryWindow window = MetricQueryWindow.recent(30, NOW);

        return ResourceMetrics.available(
                "local",
                "sample-app",
                "deployment",
                "payment-api",
                NOW,
                window,
                MetricSeries.empty("cpu", "cores"),
                MetricSeries.empty("memory", "bytes"),
                new ResourceMetricSummary(
                        cpuUsage,
                        memoryUsage,
                        cpuRequest,
                        memoryRequest,
                        null,
                        null,
                        cpuPercent,
                        memoryPercent
                )
        );
    }
}
