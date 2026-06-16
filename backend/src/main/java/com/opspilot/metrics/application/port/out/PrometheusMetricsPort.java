package com.opspilot.metrics.application.port.out;

import com.opspilot.metrics.domain.MetricQueryWindow;
import com.opspilot.metrics.domain.NodeUsageSnapshot;
import com.opspilot.metrics.domain.ResourceMetrics;
import java.util.List;

public interface PrometheusMetricsPort {

    ResourceMetrics getPodMetrics(String clusterId, String namespace, String podName, MetricQueryWindow window);

    ResourceMetrics getDeploymentMetrics(
            String clusterId,
            String namespace,
            String deploymentName,
            MetricQueryWindow window
    );

    ResourceMetrics getNodeMetrics(String clusterId, String nodeName, MetricQueryWindow window);

    NodeUsageSnapshot getNodeUsage(List<String> nodeNames);
}
