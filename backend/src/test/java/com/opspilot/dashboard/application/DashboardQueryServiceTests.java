package com.opspilot.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

import com.opspilot.dashboard.domain.DashboardExternalSignalStatus;
import com.opspilot.kafka.application.KafkaQueryService;
import com.opspilot.kafka.domain.KafkaOverview;
import com.opspilot.kubernetes.application.KubernetesInventoryService;
import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.kubernetes.domain.NamespaceSummary;
import com.opspilot.kubernetes.domain.NodeSummary;
import com.opspilot.kubernetes.domain.PodSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.metrics.application.MetricsQueryService;
import com.opspilot.metrics.domain.NodeUsageMetric;
import com.opspilot.metrics.domain.NodeUsageSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardQueryServiceTests {

    private static final Instant NOW = Instant.parse("2026-06-02T00:30:00Z");

    private KubernetesInventoryService kubernetesInventoryService;
    private MetricsQueryService metricsQueryService;
    private KafkaQueryService kafkaQueryService;
    private DashboardQueryService dashboardQueryService;

    @BeforeEach
    void setUp() {
        kubernetesInventoryService = mock(KubernetesInventoryService.class);
        metricsQueryService = mock(MetricsQueryService.class);
        kafkaQueryService = mock(KafkaQueryService.class);
        when(metricsQueryService.getNodeUsage(anyString(), anyList()))
                .thenReturn(NodeUsageSnapshot.unavailable("PROMETHEUS_METRICS_EMPTY"));
        when(kafkaQueryService.getOverview(anyString()))
                .thenReturn(KafkaOverview.unavailable("local", "KAFKA_ADMIN_QUERY_FAILED", NOW));
        dashboardQueryService = new DashboardQueryService(
                kubernetesInventoryService,
                metricsQueryService,
                kafkaQueryService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void aggregatesDashboardInventoryAndRecentSignals() {
        when(kubernetesInventoryService.getNamespaces("local"))
                .thenReturn(List.of(namespace("sample-app")));
        when(kubernetesInventoryService.getNodes("local"))
                .thenReturn(List.of(node("demo-control-plane", ResourceStatus.HEALTHY)));
        when(kubernetesInventoryService.getDeployments("local", "sample-app"))
                .thenReturn(List.of(deployment("payment-api", ResourceStatus.WARNING)));
        when(kubernetesInventoryService.getPods("local", "sample-app"))
                .thenReturn(List.of(
                        pod("worker-1", ResourceStatus.CRITICAL, "CrashLoopBackOff", 4),
                        pod("payment-api-1", ResourceStatus.HEALTHY, "Healthy", 0)
                ));
        when(kubernetesInventoryService.getEvents("local", "sample-app"))
                .thenReturn(List.of(
                        event("recent-warning", NOW.minusSeconds(60)),
                        event("old-warning", NOW.minusSeconds(3600))
                ));
        when(metricsQueryService.getNodeUsage("local", List.of("demo-control-plane")))
                .thenReturn(NodeUsageSnapshot.available(List.of(
                        new NodeUsageMetric("demo-control-plane", 0.42, 512L * 1024 * 1024, 21.0, 50.0)
                )));

        var dashboard = dashboardQueryService.getDashboard("local", null);

        assertThat(dashboard.clusterStatus()).isEqualTo(ResourceStatus.CRITICAL);
        assertThat(dashboard.counts().namespaceCount()).isEqualTo(1);
        assertThat(dashboard.counts().deploymentCount()).isEqualTo(1);
        assertThat(dashboard.counts().podCount()).isEqualTo(2);
        assertThat(dashboard.counts().criticalWorkloadCount()).isEqualTo(1);
        assertThat(dashboard.counts().warningWorkloadCount()).isEqualTo(1);
        assertThat(dashboard.counts().recentWarningEventCount()).isEqualTo(1);
        assertThat(dashboard.recentWarningEvents()).extracting(EventSummary::name)
                .containsExactly("recent-warning");
        assertThat(dashboard.unhealthyWorkloads()).extracting(workload -> workload.name())
                .containsExactly("worker-1", "payment-api");
        assertThat(dashboard.restartCountTopPods()).singleElement()
                .satisfies(pod -> assertThat(pod.restartCount()).isEqualTo(4));
        assertThat(dashboard.namespaces()).singleElement()
                .satisfies(namespace -> assertThat(namespace.status()).isEqualTo(ResourceStatus.CRITICAL));
        assertThat(dashboard.nodeUsage().status()).isEqualTo(DashboardExternalSignalStatus.AVAILABLE);
        assertThat(dashboard.nodeUsage().metrics()).singleElement()
                .satisfies(metric -> {
                    assertThat(metric.name()).isEqualTo("demo-control-plane");
                    assertThat(metric.cpuUsagePercent()).isEqualTo(21.0);
                });
        assertThat(dashboard.kafkaLag().status()).isEqualTo(DashboardExternalSignalStatus.UNAVAILABLE);
        assertThat(dashboard.cost().status()).isEqualTo(DashboardExternalSignalStatus.UNAVAILABLE);
    }

    @Test
    void narrowsWorkloadQueriesToSelectedNamespace() {
        when(kubernetesInventoryService.getNamespaces("local"))
                .thenReturn(List.of(namespace("sample-app"), namespace("kube-system")));
        when(kubernetesInventoryService.getNodes("local"))
                .thenReturn(List.of(node("demo-control-plane", ResourceStatus.HEALTHY)));
        when(kubernetesInventoryService.getDeployments("local", "sample-app"))
                .thenReturn(List.of(deployment("payment-api", ResourceStatus.HEALTHY)));
        when(kubernetesInventoryService.getPods("local", "sample-app")).thenReturn(List.of());
        when(kubernetesInventoryService.getEvents("local", "sample-app")).thenReturn(List.of());

        var dashboard = dashboardQueryService.getDashboard("local", " sample-app ");

        assertThat(dashboard.selectedNamespace()).isEqualTo("sample-app");
        assertThat(dashboard.counts().namespaceCount()).isEqualTo(2);
        assertThat(dashboard.counts().deploymentCount()).isEqualTo(1);
        assertThat(dashboard.namespaces()).extracting(namespace -> namespace.name())
                .containsExactly("sample-app");
    }

    @Test
    void keepsActiveNamespaceHealthyWhenItHasNoWorkloads() {
        when(kubernetesInventoryService.getNamespaces("local"))
                .thenReturn(List.of(namespace("default")));
        when(kubernetesInventoryService.getNodes("local"))
                .thenReturn(List.of(node("demo-control-plane", ResourceStatus.HEALTHY)));
        when(kubernetesInventoryService.getDeployments("local", "default")).thenReturn(List.of());
        when(kubernetesInventoryService.getPods("local", "default")).thenReturn(List.of());
        when(kubernetesInventoryService.getEvents("local", "default")).thenReturn(List.of());

        var dashboard = dashboardQueryService.getDashboard("local", null);

        assertThat(dashboard.namespaces()).singleElement()
                .satisfies(namespace -> assertThat(namespace.status()).isEqualTo(ResourceStatus.HEALTHY));
    }

    private NamespaceSummary namespace(String name) {
        return new NamespaceSummary("Namespace", name, ResourceStatus.HEALTHY, "Healthy", "Resource is healthy", NOW);
    }

    private NodeSummary node(String name, ResourceStatus status) {
        return new NodeSummary("Node", name, status, "Healthy", "Resource is healthy", NOW, false, "v1.35.0");
    }

    private DeploymentSummary deployment(String name, ResourceStatus status) {
        return new DeploymentSummary(
                "Deployment",
                "sample-app",
                name,
                status,
                status == ResourceStatus.HEALTHY ? "Healthy" : "UnavailableReplicas",
                "Deployment status",
                NOW,
                1,
                status == ResourceStatus.HEALTHY ? 1 : 0,
                status == ResourceStatus.HEALTHY ? 1 : 0,
                1
        );
    }

    private PodSummary pod(String name, ResourceStatus status, String reason, int restartCount) {
        return new PodSummary(
                "Pod",
                "sample-app",
                name,
                status,
                reason,
                "Pod status",
                NOW,
                "Running",
                "demo-control-plane",
                restartCount,
                List.of("busybox:1.37.0")
        );
    }

    private EventSummary event(String name, Instant lastUpdatedAt) {
        return new EventSummary(
                "Event",
                "sample-app",
                name,
                ResourceStatus.WARNING,
                "BackOff",
                "Back-off restarting failed container",
                lastUpdatedAt,
                "Warning",
                "Pod",
                "worker-1",
                1
        );
    }
}
