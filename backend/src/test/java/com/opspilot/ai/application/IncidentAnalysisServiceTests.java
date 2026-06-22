package com.opspilot.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.opspilot.ai.adapter.out.memory.InMemoryIncidentAnalysisStore;
import com.opspilot.ai.adapter.out.stub.StubAiIncidentAnalysisAdapter;
import com.opspilot.ai.domain.IncidentAnalysisRequest;
import com.opspilot.kafka.application.KafkaQueryService;
import com.opspilot.kafka.domain.KafkaConsumerGroupLag;
import com.opspilot.kafka.domain.KafkaPartitionLag;
import com.opspilot.kubernetes.application.KubernetesInventoryService;
import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.metrics.application.MetricsQueryService;
import com.opspilot.metrics.domain.MetricQueryWindow;
import com.opspilot.metrics.domain.ResourceMetrics;
import com.opspilot.topology.application.TopologyQueryService;
import com.opspilot.topology.domain.TopologyGraph;
import com.opspilot.topology.domain.TopologyNode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IncidentAnalysisServiceTests {

    private static final Instant NOW = Instant.parse("2026-06-04T00:30:00Z");

    private KubernetesInventoryService kubernetesInventoryService;
    private MetricsQueryService metricsQueryService;
    private TopologyQueryService topologyQueryService;
    private KafkaQueryService kafkaQueryService;
    private IncidentAnalysisService incidentAnalysisService;

    @BeforeEach
    void setUp() {
        kubernetesInventoryService = mock(KubernetesInventoryService.class);
        metricsQueryService = mock(MetricsQueryService.class);
        topologyQueryService = mock(TopologyQueryService.class);
        kafkaQueryService = mock(KafkaQueryService.class);
        incidentAnalysisService = new IncidentAnalysisService(
                kubernetesInventoryService,
                metricsQueryService,
                topologyQueryService,
                kafkaQueryService,
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

    @Test
    void includesKafkaLagEvidenceWhenTopologyHasConsumerGroup() {
        when(kubernetesInventoryService.getDeployments("local", "sample-app"))
                .thenReturn(List.of(new DeploymentSummary(
                        "Deployment",
                        "sample-app",
                        "order-consumer",
                        ResourceStatus.HEALTHY,
                        "Available",
                        "Available replicas 1/1",
                        NOW,
                        1,
                        1,
                        1,
                        1
                )));
        when(kubernetesInventoryService.getEvents("local", "sample-app")).thenReturn(List.of());
        when(topologyQueryService.getTopology("local", "sample-app", "Deployment", "order-consumer"))
                .thenReturn(new TopologyGraph(
                        "local",
                        "sample-app",
                        "deployment:sample-app:order-consumer",
                        NOW,
                        List.of(
                                new TopologyNode(
                                        "deployment:sample-app:order-consumer",
                                        "Deployment",
                                        "sample-app",
                                        "order-consumer",
                                        ResourceStatus.HEALTHY,
                                        "Available",
                                        "Available replicas 1/1"
                                ),
                                new TopologyNode(
                                        "kafkaconsumergroup:order-consumer",
                                        "KafkaConsumerGroup",
                                        null,
                                        "order-consumer",
                                        ResourceStatus.UNKNOWN,
                                        "MetadataOnly",
                                        "Dependency inferred from workload annotation"
                                )
                        ),
                        List.of()
                ));
        when(kafkaQueryService.getConsumerGroupLag("local", "order-consumer"))
                .thenReturn(new KafkaConsumerGroupLag(
                        "order-consumer",
                        ResourceStatus.WARNING,
                        "KAFKA_LAG_WARNING",
                        42,
                        NOW,
                        List.of(new KafkaPartitionLag("orders.created", 0, 10L, 52L, 42))
                ));

        var report = incidentAnalysisService.analyzeIncident(
                "local",
                new IncidentAnalysisRequest("sample-app", "Deployment", "order-consumer", 30)
        );

        assertThat(report.severity()).isEqualTo(ResourceStatus.WARNING);
        assertThat(report.evidence()).extracting(evidence -> evidence.type()).contains("kafka");
        assertThat(report.rootCauseCandidates()).extracting(candidate -> candidate.title())
                .anyMatch(title -> title.contains("Kafka consumer"));
    }
}
