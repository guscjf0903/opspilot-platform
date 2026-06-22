package com.opspilot.ai.application;

import com.opspilot.ai.application.port.out.AiIncidentAnalysisPort;
import com.opspilot.ai.application.port.out.IncidentAnalysisStore;
import com.opspilot.ai.domain.IncidentAnalysisContext;
import com.opspilot.ai.domain.IncidentAnalysisReport;
import com.opspilot.ai.domain.IncidentAnalysisRequest;
import com.opspilot.ai.domain.IncidentAnalysisTarget;
import com.opspilot.kafka.application.KafkaQueryService;
import com.opspilot.kafka.domain.KafkaConsumerGroupLag;
import com.opspilot.kubernetes.application.KubernetesInventoryService;
import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.kubernetes.domain.NamespaceSummary;
import com.opspilot.kubernetes.domain.NodeSummary;
import com.opspilot.kubernetes.domain.PodSummary;
import com.opspilot.metrics.application.MetricsQueryService;
import com.opspilot.metrics.domain.ResourceMetrics;
import com.opspilot.topology.application.TopologyQueryService;
import com.opspilot.topology.application.TopologyResourceNotFoundException;
import com.opspilot.topology.domain.TopologyGraph;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IncidentAnalysisService {

    private static final int DEFAULT_TIME_RANGE_MINUTES = 30;
    private static final int MIN_TIME_RANGE_MINUTES = 5;
    private static final int MAX_TIME_RANGE_MINUTES = 180;
    private static final int EVENT_LIMIT = 10;

    private final KubernetesInventoryService kubernetesInventoryService;
    private final MetricsQueryService metricsQueryService;
    private final TopologyQueryService topologyQueryService;
    private final KafkaQueryService kafkaQueryService;
    private final AiIncidentAnalysisPort aiIncidentAnalysisPort;
    private final IncidentAnalysisStore incidentAnalysisStore;

    public IncidentAnalysisReport analyzeIncident(String clusterId, IncidentAnalysisRequest request) {
        int timeRangeMinutes = normalizeTimeRange(request.timeRangeMinutes());
        IncidentAnalysisTarget target = resolveTarget(clusterId, request);
        List<EventSummary> events = collectRelatedEvents(clusterId, request.namespace(), target);
        ResourceMetrics metrics = collectMetrics(clusterId, target, timeRangeMinutes);
        TopologyGraph topology = collectTopology(clusterId, target);
        List<KafkaConsumerGroupLag> kafkaConsumerGroupLags = collectKafkaConsumerGroupLags(clusterId, topology);
        IncidentAnalysisContext context = new IncidentAnalysisContext(
                clusterId,
                target,
                timeRangeMinutes,
                Instant.now(),
                events,
                kafkaConsumerGroupLags,
                metrics,
                topology
        );

        return incidentAnalysisStore.save(aiIncidentAnalysisPort.analyze(context));
    }

    public IncidentAnalysisReport getIncidentAnalysis(UUID analysisId) {
        return incidentAnalysisStore.findById(analysisId)
                .orElseThrow(() -> new IncidentAnalysisNotFoundException(analysisId));
    }

    public List<IncidentAnalysisReport> getIncidentAnalyses(String clusterId) {
        return incidentAnalysisStore.findByClusterId(clusterId);
    }

    private IncidentAnalysisTarget resolveTarget(String clusterId, IncidentAnalysisRequest request) {
        String kind = normalizeKind(request.targetKind());

        return switch (kind) {
            case "deployment" -> kubernetesInventoryService.getDeployments(clusterId, request.namespace()).stream()
                    .filter(deployment -> deployment.name().equals(request.targetName()))
                    .findFirst()
                    .map(this::toTarget)
                    .orElseThrow(() -> new AnalysisTargetNotFoundException(request.targetKind(), request.targetName()));
            case "pod" -> kubernetesInventoryService.getPods(clusterId, request.namespace()).stream()
                    .filter(pod -> pod.name().equals(request.targetName()))
                    .findFirst()
                    .map(this::toTarget)
                    .orElseThrow(() -> new AnalysisTargetNotFoundException(request.targetKind(), request.targetName()));
            case "node" -> kubernetesInventoryService.getNodes(clusterId).stream()
                    .filter(node -> node.name().equals(request.targetName()))
                    .findFirst()
                    .map(this::toTarget)
                    .orElseThrow(() -> new AnalysisTargetNotFoundException(request.targetKind(), request.targetName()));
            case "namespace" -> kubernetesInventoryService.getNamespaces(clusterId).stream()
                    .filter(namespace -> namespace.name().equals(request.targetName()))
                    .findFirst()
                    .map(this::toTarget)
                    .orElseThrow(() -> new AnalysisTargetNotFoundException(request.targetKind(), request.targetName()));
            default -> throw new UnsupportedAnalysisTargetException(request.targetKind());
        };
    }

    private List<EventSummary> collectRelatedEvents(String clusterId, String namespace, IncidentAnalysisTarget target) {
        return kubernetesInventoryService.getEvents(clusterId, namespace).stream()
                .filter(event -> isRelatedEvent(event, target))
                .sorted(Comparator.comparing(
                        EventSummary::lastUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(EVENT_LIMIT)
                .toList();
    }

    private ResourceMetrics collectMetrics(String clusterId, IncidentAnalysisTarget target, int timeRangeMinutes) {
        String kind = normalizeKind(target.kind());

        return switch (kind) {
            case "deployment", "pod" -> metricsQueryService.getWorkloadMetrics(
                    clusterId,
                    target.namespace(),
                    kind,
                    target.name(),
                    timeRangeMinutes
            );
            case "node" -> metricsQueryService.getNodeMetrics(clusterId, target.name(), timeRangeMinutes);
            default -> null;
        };
    }

    private TopologyGraph collectTopology(String clusterId, IncidentAnalysisTarget target) {
        String kind = normalizeKind(target.kind());

        if (!"deployment".equals(kind) && !"pod".equals(kind)) {
            return null;
        }

        try {
            return topologyQueryService.getTopology(clusterId, target.namespace(), target.kind(), target.name());
        } catch (TopologyResourceNotFoundException exception) {
            return null;
        }
    }

    private List<KafkaConsumerGroupLag> collectKafkaConsumerGroupLags(String clusterId, TopologyGraph topology) {
        if (topology == null) {
            return List.of();
        }

        Set<String> consumerGroupIds = topology.nodes().stream()
                .filter(node -> "KafkaConsumerGroup".equals(node.kind()))
                .map(node -> node.name())
                .collect(Collectors.toSet());
        if (consumerGroupIds.isEmpty()) {
            return List.of();
        }

        return consumerGroupIds.stream()
                .sorted()
                .map(groupId -> kafkaQueryService.getConsumerGroupLag(clusterId, groupId))
                .toList();
    }

    private boolean isRelatedEvent(EventSummary event, IncidentAnalysisTarget target) {
        if (event.involvedName() == null) {
            return false;
        }

        return event.involvedName().equals(target.name())
                || event.involvedName().startsWith(target.name() + "-")
                || event.message().contains(target.name());
    }

    private IncidentAnalysisTarget toTarget(DeploymentSummary deployment) {
        return new IncidentAnalysisTarget(
                deployment.namespace(),
                deployment.kind(),
                deployment.name(),
                deployment.status(),
                deployment.reason(),
                deployment.message(),
                deployment.lastUpdatedAt()
        );
    }

    private IncidentAnalysisTarget toTarget(PodSummary pod) {
        return new IncidentAnalysisTarget(
                pod.namespace(),
                pod.kind(),
                pod.name(),
                pod.status(),
                pod.reason(),
                pod.message(),
                pod.lastUpdatedAt()
        );
    }

    private IncidentAnalysisTarget toTarget(NodeSummary node) {
        return new IncidentAnalysisTarget(
                null,
                node.kind(),
                node.name(),
                node.status(),
                node.reason(),
                node.message(),
                node.lastUpdatedAt()
        );
    }

    private IncidentAnalysisTarget toTarget(NamespaceSummary namespace) {
        return new IncidentAnalysisTarget(
                namespace.name(),
                namespace.kind(),
                namespace.name(),
                namespace.status(),
                namespace.reason(),
                namespace.message(),
                namespace.lastUpdatedAt()
        );
    }

    private String normalizeKind(String kind) {
        return kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
    }

    private int normalizeTimeRange(Integer timeRangeMinutes) {
        int normalized = timeRangeMinutes == null ? DEFAULT_TIME_RANGE_MINUTES : timeRangeMinutes;

        return Math.max(MIN_TIME_RANGE_MINUTES, Math.min(MAX_TIME_RANGE_MINUTES, normalized));
    }
}
