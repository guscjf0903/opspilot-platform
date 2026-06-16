package com.opspilot.metrics.adapter.out.prometheus;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.metrics.application.MetricsUnavailableException;
import com.opspilot.metrics.application.port.out.PrometheusMetricsPort;
import com.opspilot.metrics.domain.MetricPoint;
import com.opspilot.metrics.domain.MetricQueryWindow;
import com.opspilot.metrics.domain.MetricSeries;
import com.opspilot.metrics.domain.NodeUsageMetric;
import com.opspilot.metrics.domain.NodeUsageSnapshot;
import com.opspilot.metrics.domain.ResourceMetricSummary;
import com.opspilot.metrics.domain.ResourceMetrics;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

@Component
@RequiredArgsConstructor
public class PrometheusHttpMetricsAdapter implements PrometheusMetricsPort {

    private static final String CPU_UNIT = "cores";
    private static final String MEMORY_UNIT = "bytes";
    private static final String PROMETHEUS_QUERY_FAILED = "PROMETHEUS_QUERY_FAILED";
    private static final String PROMETHEUS_METRICS_EMPTY = "PROMETHEUS_METRICS_EMPTY";

    private final RestClient prometheusRestClient;

    @Override
    public ResourceMetrics getPodMetrics(
            String clusterId,
            String namespace,
            String podName,
            MetricQueryWindow window
    ) {
        String namespaceLabel = labelValue(namespace);
        String podLabel = labelValue(podName);
        String podSelector = "namespace=\"" + namespaceLabel + "\",pod=\"" + podLabel + "\"";
        MetricSeries cpu = queryRange(cpuUsageQuery(podSelector), window, "cpu", CPU_UNIT);
        MetricSeries memory = queryRange(memoryUsageQuery(podSelector), window, "memory", MEMORY_UNIT);
        ResourceMetricSummary summary = workloadSummary(
                cpu,
                memory,
                cpuRequestQuery(podSelector),
                memoryRequestQuery(podSelector)
        );

        return ResourceMetrics.available(clusterId, namespace, "pod", podName, window.end(), window, cpu, memory, summary);
    }

    @Override
    public ResourceMetrics getDeploymentMetrics(
            String clusterId,
            String namespace,
            String deploymentName,
            MetricQueryWindow window
    ) {
        String namespaceLabel = labelValue(namespace);
        String podRegex = labelValue(regexLiteral(deploymentName) + "-.+");
        String podSelector = "namespace=\"" + namespaceLabel + "\",pod=~\"" + podRegex + "\"";
        MetricSeries cpu = queryRange(cpuUsageQuery(podSelector), window, "cpu", CPU_UNIT);
        MetricSeries memory = queryRange(memoryUsageQuery(podSelector), window, "memory", MEMORY_UNIT);
        ResourceMetricSummary summary = workloadSummary(
                cpu,
                memory,
                cpuRequestQuery(podSelector),
                memoryRequestQuery(podSelector)
        );

        return ResourceMetrics.available(
                clusterId,
                namespace,
                "deployment",
                deploymentName,
                window.end(),
                window,
                cpu,
                memory,
                summary
        );
    }

    @Override
    public ResourceMetrics getNodeMetrics(String clusterId, String nodeName, MetricQueryWindow window) {
        String nodeLabel = labelValue(nodeName);
        MetricSeries cpu = queryRange(nodeCpuUsageQuery(nodeLabel), window, "cpu", CPU_UNIT);
        MetricSeries memory = queryRange(nodeMemoryUsageQuery(nodeLabel), window, "memory", MEMORY_UNIT);
        Double cpuCapacity = queryScalar(nodeCpuCapacityQuery(nodeLabel)).orElse(null);
        Long memoryCapacity = queryScalar(nodeMemoryCapacityQuery(nodeLabel)).map(Math::round).orElse(null);
        Double latestCpuUsage = latestValue(cpu).orElse(null);
        Long latestMemoryUsage = latestValue(memory).map(Math::round).orElse(null);
        ResourceMetricSummary summary = new ResourceMetricSummary(
                latestCpuUsage,
                latestMemoryUsage,
                null,
                null,
                cpuCapacity,
                memoryCapacity,
                percent(latestCpuUsage, cpuCapacity),
                percent(latestMemoryUsage, memoryCapacity)
        );

        return ResourceMetrics.available(clusterId, null, "node", nodeName, window.end(), window, cpu, memory, summary);
    }

    @Override
    public NodeUsageSnapshot getNodeUsage(List<String> nodeNames) {
        Map<String, Double> cpuUsage = queryVectorByLabel(clusterCpuUsageQuery(), "node");
        Map<String, Double> memoryUsage = queryVectorByLabel(clusterMemoryUsageQuery(), "node");
        Map<String, Double> cpuCapacity = queryVectorByLabel(clusterCpuCapacityQuery(), "node");
        Map<String, Double> memoryCapacity = queryVectorByLabel(clusterMemoryCapacityQuery(), "node");

        List<NodeUsageMetric> nodes = nodeNames.stream()
                .map(nodeName -> {
                    Double nodeCpuUsage = cpuUsage.get(nodeName);
                    Long nodeMemoryUsage = Optional.ofNullable(memoryUsage.get(nodeName))
                            .map(Math::round)
                            .orElse(null);
                    Double nodeCpuCapacity = cpuCapacity.get(nodeName);
                    Long nodeMemoryCapacity = Optional.ofNullable(memoryCapacity.get(nodeName))
                            .map(Math::round)
                            .orElse(null);

                    return new NodeUsageMetric(
                            nodeName,
                            nodeCpuUsage,
                            nodeMemoryUsage,
                            percent(nodeCpuUsage, nodeCpuCapacity),
                            percent(nodeMemoryUsage, nodeMemoryCapacity)
                    );
                })
                .filter(node -> node.cpuUsageCores() != null || node.memoryUsageBytes() != null)
                .toList();

        if (nodes.isEmpty()) {
            return NodeUsageSnapshot.unavailable(PROMETHEUS_METRICS_EMPTY);
        }

        return NodeUsageSnapshot.available(nodes);
    }

    private ResourceMetricSummary workloadSummary(
            MetricSeries cpu,
            MetricSeries memory,
            String cpuRequestQuery,
            String memoryRequestQuery
    ) {
        Double latestCpuUsage = latestValue(cpu).orElse(null);
        Long latestMemoryUsage = latestValue(memory).map(Math::round).orElse(null);
        Double cpuRequest = queryScalar(cpuRequestQuery).orElse(null);
        Long memoryRequest = queryScalar(memoryRequestQuery).map(Math::round).orElse(null);

        return new ResourceMetricSummary(
                latestCpuUsage,
                latestMemoryUsage,
                cpuRequest,
                memoryRequest,
                null,
                null,
                percent(latestCpuUsage, cpuRequest),
                percent(latestMemoryUsage, memoryRequest)
        );
    }

    private MetricSeries queryRange(String query, MetricQueryWindow window, String name, String unit) {
        JsonNode body = getPrometheusBody("/api/v1/query_range", query, window);
        JsonNode results = body.path("data").path("result");
        Map<Instant, Double> valuesByTimestamp = new HashMap<>();

        if (results.isArray()) {
            for (JsonNode result : results) {
                JsonNode values = result.path("values");

                if (!values.isArray()) {
                    continue;
                }

                for (JsonNode value : values) {
                    parseMetricValue(value).ifPresent(point ->
                            valuesByTimestamp.merge(point.timestamp(), point.value(), Double::sum));
                }
            }
        }

        List<MetricPoint> points = valuesByTimestamp.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MetricPoint(entry.getKey(), entry.getValue()))
                .toList();

        return new MetricSeries(name, unit, points);
    }

    private Optional<Double> queryScalar(String query) {
        JsonNode body = getPrometheusBody("/api/v1/query", query, null);
        JsonNode results = body.path("data").path("result");

        if (!results.isArray()) {
            return Optional.empty();
        }

        double sum = 0.0;
        boolean found = false;

        for (JsonNode result : results) {
            Optional<Double> value = parseMetricValue(result.path("value")).map(MetricPoint::value);

            if (value.isPresent()) {
                sum += value.get();
                found = true;
            }
        }

        return found ? Optional.of(sum) : Optional.empty();
    }

    private Map<String, Double> queryVectorByLabel(String query, String labelName) {
        JsonNode body = getPrometheusBody("/api/v1/query", query, null);
        JsonNode results = body.path("data").path("result");
        Map<String, Double> values = new HashMap<>();

        if (!results.isArray()) {
            return values;
        }

        for (JsonNode result : results) {
            String labelValue = result.path("metric").path(labelName).asText(null);
            Optional<Double> value = parseMetricValue(result.path("value")).map(MetricPoint::value);

            if (labelValue != null && value.isPresent()) {
                values.merge(labelValue, value.get(), Double::sum);
            }
        }

        return values;
    }

    private JsonNode getPrometheusBody(String path, String query, MetricQueryWindow window) {
        try {
            JsonNode body = prometheusRestClient.get()
                    .uri(uriBuilder -> prometheusUri(uriBuilder, path, query, window))
                    .retrieve()
                    .body(JsonNode.class);

            if (body == null || !"success".equals(body.path("status").asText())) {
                throw new MetricsUnavailableException(PROMETHEUS_QUERY_FAILED);
            }

            return body;
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new MetricsUnavailableException(PROMETHEUS_QUERY_FAILED, exception);
        }
    }

    private URI prometheusUri(UriBuilder uriBuilder, String path, String query, MetricQueryWindow window) {
        UriBuilder builder = uriBuilder
                .path(path)
                .queryParam("query", "{query}");
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);

        if (window != null) {
            builder
                    .queryParam("start", "{start}")
                    .queryParam("end", "{end}")
                    .queryParam("step", "{step}");
            variables.put("start", window.start().getEpochSecond());
            variables.put("end", window.end().getEpochSecond());
            variables.put("step", window.step().toSeconds());
        }

        return builder.build(variables);
    }

    private Optional<MetricPoint> parseMetricValue(JsonNode value) {
        if (!value.isArray() || value.size() < 2) {
            return Optional.empty();
        }

        double epochSeconds = value.get(0).asDouble();
        String rawValue = value.get(1).asText();

        try {
            double parsedValue = Double.parseDouble(rawValue);

            if (!Double.isFinite(parsedValue)) {
                return Optional.empty();
            }

            long epochMillis = Math.round(epochSeconds * 1000);

            return Optional.of(new MetricPoint(Instant.ofEpochMilli(epochMillis), parsedValue));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private Optional<Double> latestValue(MetricSeries series) {
        return series.points().stream()
                .max(Comparator.comparing(MetricPoint::timestamp))
                .map(MetricPoint::value);
    }

    private Double percent(Number usage, Number baseline) {
        if (usage == null || baseline == null || baseline.doubleValue() <= 0.0) {
            return null;
        }

        return usage.doubleValue() / baseline.doubleValue() * 100.0;
    }

    private String cpuUsageQuery(String selector) {
        return "sum(rate(container_cpu_usage_seconds_total{"
                + selector
                + ",container!=\"\",image!=\"\"}[5m]))";
    }

    private String memoryUsageQuery(String selector) {
        return "sum(container_memory_working_set_bytes{"
                + selector
                + ",container!=\"\",image!=\"\"})";
    }

    private String cpuRequestQuery(String selector) {
        return "sum(kube_pod_container_resource_requests{"
                + selector
                + ",resource=\"cpu\",unit=\"core\"})";
    }

    private String memoryRequestQuery(String selector) {
        return "sum(kube_pod_container_resource_requests{"
                + selector
                + ",resource=\"memory\",unit=\"byte\"})";
    }

    private String nodeCpuUsageQuery(String nodeLabel) {
        return "(1 - avg by (node) (rate(node_cpu_seconds_total{node=\""
                + nodeLabel
                + "\",mode=\"idle\"}[5m]))) * count by (node) (count by (node, cpu) "
                + "(node_cpu_seconds_total{node=\""
                + nodeLabel
                + "\",mode=\"idle\"}))";
    }

    private String nodeMemoryUsageQuery(String nodeLabel) {
        return "node_memory_MemTotal_bytes{node=\""
                + nodeLabel
                + "\"} - node_memory_MemAvailable_bytes{node=\""
                + nodeLabel
                + "\"}";
    }

    private String nodeCpuCapacityQuery(String nodeLabel) {
        return "count by (node) (count by (node, cpu) (node_cpu_seconds_total{node=\""
                + nodeLabel
                + "\",mode=\"idle\"}))";
    }

    private String nodeMemoryCapacityQuery(String nodeLabel) {
        return "node_memory_MemTotal_bytes{node=\"" + nodeLabel + "\"}";
    }

    private String clusterCpuUsageQuery() {
        return "(1 - avg by (node) (rate(node_cpu_seconds_total{mode=\"idle\"}[5m]))) "
                + "* count by (node) (count by (node, cpu) (node_cpu_seconds_total{mode=\"idle\"}))";
    }

    private String clusterMemoryUsageQuery() {
        return "node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes";
    }

    private String clusterCpuCapacityQuery() {
        return "count by (node) (count by (node, cpu) (node_cpu_seconds_total{mode=\"idle\"}))";
    }

    private String clusterMemoryCapacityQuery() {
        return "node_memory_MemTotal_bytes";
    }

    private String labelValue(String value) {
        return value == null
                ? ""
                : value
                        .replace("\\", "\\\\")
                        .replace("\n", "\\n")
                        .replace("\"", "\\\"");
    }

    private String regexLiteral(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        List<Character> regexCharacters = List.of('\\', '.', '^', '$', '|', '?', '*', '+', '(', ')', '[', ']', '{', '}');
        List<Character> escaped = new ArrayList<>();

        for (char character : value.toCharArray()) {
            if (regexCharacters.contains(character)) {
                escaped.add('\\');
            }

            escaped.add(character);
        }

        StringBuilder builder = new StringBuilder(escaped.size());
        escaped.forEach(builder::append);

        return builder.toString();
    }
}
