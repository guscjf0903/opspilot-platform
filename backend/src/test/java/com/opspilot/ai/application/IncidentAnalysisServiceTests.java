package com.opspilot.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.opspilot.ai.adapter.out.memory.InMemoryIncidentAnalysisStore;
import com.opspilot.ai.adapter.out.stub.StubAiIncidentAnalysisAdapter;
import com.opspilot.ai.domain.IncidentAnalysisRequest;
import com.opspilot.kubernetes.application.KubernetesInventoryService;
import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.metrics.application.MetricsQueryService;
import com.opspilot.metrics.domain.MetricQueryWindow;
import com.opspilot.metrics.domain.ResourceMetrics;
import com.opspilot.topology.application.TopologyQueryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IncidentAnalysisServiceTests {

    private static final Instant NOW = Instant.parse("2026-06-04T00:30:00Z");

    private KubernetesInventoryService kubernetesInventoryService;
    private MetricsQueryService metricsQueryService;
    private IncidentAnalysisService incidentAnalysisService;

    @BeforeEach
    void setUp() {
        kubernetesInventoryService = mock(KubernetesInventoryService.class);
        metricsQueryService = mock(MetricsQueryService.class);
        TopologyQueryService topologyQueryService = mock(TopologyQueryService.class);
        incidentAnalysisService = new IncidentAnalysisService(
                kubernetesInventoryService,
                metricsQueryService,
                topologyQueryService,
                new StubAiIncidentAnalysisAdapter(),
                new InMemoryIncidentAnalysisStore()
        );
    }

    @Test
    void analyzesDeploymentWithEventsAndMetrics() {
        when(kubernetesInventoryService.getDeployments("local", "sample-app"))
                .thenReturn(List.of(new DeploymentSummary(
                        "Deployment",
                        "sample-app",
                        "payment-api",
                        ResourceStatus.WARNING,
                        "UnavailableReplicas",
                        "Available replicas 1/2",
                        NOW,
                        2,
                        1,
                        1,
                        2
                )));
        when(kubernetesInventoryService.getEvents("local", "sample-app"))
                .thenReturn(List.of(new EventSummary(
                        "Event",
                        "sample-app",
                        "payment-api-warning",
                        ResourceStatus.WARNING,
                        "Unhealthy",
                        "Readiness probe failed",
                        NOW,
                        "Warning",
                        "Pod",
                        "payment-api-abc123",
                        3
                )));
        when(metricsQueryService.getWorkloadMetrics(eq("local"), eq("sample-app"), eq("deployment"), eq("payment-api"), anyInt()))
                .thenReturn(ResourceMetrics.unavailable(
                        "PROMETHEUS_QUERY_FAILED",
                        "local",
                        "sample-app",
                        "deployment",
                        "payment-api",
                        NOW,
                        MetricQueryWindow.recent(30, NOW)
                ));

        var report = incidentAnalysisService.analyzeIncident(
                "local",
                new IncidentAnalysisRequest("sample-app", "Deployment", "payment-api", 30)
        );

        assertThat(report.analysisId()).isNotNull();
        assertThat(report.severity()).isEqualTo(ResourceStatus.WARNING);
        assertThat(report.rootCauseCandidates()).isNotEmpty();
        assertThat(report.evidence()).extracting(evidence -> evidence.type())
                .contains("target", "event", "metric");
        assertThat(incidentAnalysisService.getIncidentAnalysis(report.analysisId()))
                .isEqualTo(report);
    }
}
