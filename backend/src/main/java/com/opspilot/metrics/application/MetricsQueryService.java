package com.opspilot.metrics.application;

import com.opspilot.kubernetes.config.OpspilotKubernetesProperties;
import com.opspilot.metrics.application.port.out.PrometheusMetricsPort;
import com.opspilot.metrics.config.OpspilotPrometheusProperties;
import com.opspilot.metrics.domain.MetricQueryWindow;
import com.opspilot.metrics.domain.NodeUsageSnapshot;
import com.opspilot.metrics.domain.ResourceMetrics;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsQueryService {

    private static final String POD_KIND = "pod";
    private static final String DEPLOYMENT_KIND = "deployment";

    private final PrometheusMetricsPort prometheusMetricsPort;
    private final OpspilotKubernetesProperties kubernetesProperties;
    private final OpspilotPrometheusProperties prometheusProperties;

    public ResourceMetrics getWorkloadMetrics(
            String clusterId,
            String namespace,
            String kind,
            String name,
            Integer rangeMinutes
    ) {
        validateCluster(clusterId);
        MetricQueryWindow window = MetricQueryWindow.recent(rangeMinutes, Instant.now());
        String normalizedKind = normalizeKind(kind);

        if (!prometheusProperties.isEnabled()) {
            return ResourceMetrics.unavailable(
                    "PROMETHEUS_DISABLED",
                    clusterId,
                    namespace,
                    normalizedKind,
                    name,
                    window.end(),
                    window
            );
        }

        try {
            return switch (normalizedKind) {
                case POD_KIND -> prometheusMetricsPort.getPodMetrics(clusterId, namespace, name, window);
                case DEPLOYMENT_KIND -> prometheusMetricsPort.getDeploymentMetrics(clusterId, namespace, name, window);
                default -> throw new UnsupportedMetricResourceException(kind);
            };
        } catch (MetricsUnavailableException exception) {
            return ResourceMetrics.unavailable(
                    exception.getMessage(),
                    clusterId,
                    namespace,
                    normalizedKind,
                    name,
                    window.end(),
                    window
            );
        }
    }

    public ResourceMetrics getNodeMetrics(String clusterId, String nodeName, Integer rangeMinutes) {
        validateCluster(clusterId);
        MetricQueryWindow window = MetricQueryWindow.recent(rangeMinutes, Instant.now());

        if (!prometheusProperties.isEnabled()) {
            return ResourceMetrics.unavailable(
                    "PROMETHEUS_DISABLED",
                    clusterId,
                    null,
                    "node",
                    nodeName,
                    window.end(),
                    window
            );
        }

        try {
            return prometheusMetricsPort.getNodeMetrics(clusterId, nodeName, window);
        } catch (MetricsUnavailableException exception) {
            return ResourceMetrics.unavailable(
                    exception.getMessage(),
                    clusterId,
                    null,
                    "node",
                    nodeName,
                    window.end(),
                    window
            );
        }
    }

    public NodeUsageSnapshot getNodeUsage(String clusterId, List<String> nodeNames) {
        validateCluster(clusterId);

        if (nodeNames.isEmpty()) {
            return NodeUsageSnapshot.unavailable("NO_NODES");
        }

        if (!prometheusProperties.isEnabled()) {
            return NodeUsageSnapshot.unavailable("PROMETHEUS_DISABLED");
        }

        try {
            return prometheusMetricsPort.getNodeUsage(nodeNames);
        } catch (MetricsUnavailableException exception) {
            return NodeUsageSnapshot.unavailable(exception.getMessage());
        }
    }

    private String normalizeKind(String kind) {
        String normalized = kind == null ? "" : kind.trim().toLowerCase();

        if (POD_KIND.equals(normalized) || DEPLOYMENT_KIND.equals(normalized)) {
            return normalized;
        }

        throw new UnsupportedMetricResourceException(kind);
    }

    private void validateCluster(String clusterId) {
        if (!kubernetesProperties.getClusterId().equals(clusterId)) {
            throw new com.opspilot.kubernetes.application.UnknownClusterException(clusterId);
        }
    }
}
