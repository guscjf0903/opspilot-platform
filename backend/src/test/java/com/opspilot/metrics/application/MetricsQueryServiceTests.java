package com.opspilot.metrics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.opspilot.kubernetes.application.UnknownClusterException;
import com.opspilot.kubernetes.config.OpspilotKubernetesProperties;
import com.opspilot.metrics.application.port.out.PrometheusMetricsPort;
import com.opspilot.metrics.config.OpspilotPrometheusProperties;
import com.opspilot.metrics.domain.MetricQueryWindow;
import com.opspilot.metrics.domain.MetricsAvailabilityStatus;
import com.opspilot.metrics.domain.ResourceMetrics;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class MetricsQueryServiceTests {

    private PrometheusMetricsPort prometheusMetricsPort;
    private OpspilotPrometheusProperties prometheusProperties;
    private MetricsQueryService metricsQueryService;

    @BeforeEach
    void setUp() {
        prometheusMetricsPort = mock(PrometheusMetricsPort.class);
        OpspilotKubernetesProperties kubernetesProperties = new OpspilotKubernetesProperties();
        prometheusProperties = new OpspilotPrometheusProperties();
        metricsQueryService = new MetricsQueryService(prometheusMetricsPort, kubernetesProperties, prometheusProperties);
    }

    @Test
    void returnsUnavailableWhenPrometheusIsDisabled() {
        prometheusProperties.setEnabled(false);

        ResourceMetrics metrics = metricsQueryService.getWorkloadMetrics(
                "local",
                "sample-app",
                "pod",
                "worker-1",
                30
        );

        assertThat(metrics.status()).isEqualTo(MetricsAvailabilityStatus.UNAVAILABLE);
        assertThat(metrics.reason()).isEqualTo("PROMETHEUS_DISABLED");
        verifyNoInteractions(prometheusMetricsPort);
    }

    @Test
    void mapsPrometheusFailuresToUnavailableMetrics() {
        when(prometheusMetricsPort.getPodMetrics(
                ArgumentMatchers.eq("local"),
                ArgumentMatchers.eq("sample-app"),
                ArgumentMatchers.eq("worker-1"),
                ArgumentMatchers.any(MetricQueryWindow.class)
        )).thenThrow(new MetricsUnavailableException("PROMETHEUS_QUERY_FAILED"));

        ResourceMetrics metrics = metricsQueryService.getWorkloadMetrics(
                "local",
                "sample-app",
                "pod",
                "worker-1",
                30
        );

        assertThat(metrics.status()).isEqualTo(MetricsAvailabilityStatus.UNAVAILABLE);
        assertThat(metrics.reason()).isEqualTo("PROMETHEUS_QUERY_FAILED");
        assertThat(metrics.cpu().points()).isEmpty();
    }

    @Test
    void rejectsUnsupportedWorkloadKind() {
        assertThatThrownBy(() -> metricsQueryService.getWorkloadMetrics(
                "local",
                "sample-app",
                "service",
                "payment-api",
                30
        )).isInstanceOf(UnsupportedMetricResourceException.class);
    }

    @Test
    void rejectsUnknownCluster() {
        assertThatThrownBy(() -> metricsQueryService.getNodeMetrics("missing", "demo-control-plane", 30))
                .isInstanceOf(UnknownClusterException.class);
    }

    @Test
    void normalizesRangeMinutes() {
        MetricQueryWindow window = MetricQueryWindow.recent(1000, Instant.parse("2026-06-02T00:30:00Z"));

        assertThat(window.rangeMinutes()).isEqualTo(180);
        assertThat(window.start()).isEqualTo(Instant.parse("2026-06-01T21:30:00Z"));
    }
}
