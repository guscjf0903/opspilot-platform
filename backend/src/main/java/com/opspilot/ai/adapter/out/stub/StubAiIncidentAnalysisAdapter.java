package com.opspilot.ai.adapter.out.stub;

import com.opspilot.ai.application.port.out.AiIncidentAnalysisPort;
import com.opspilot.ai.domain.AiTokenUsage;
import com.opspilot.ai.domain.IncidentAnalysisContext;
import com.opspilot.ai.domain.IncidentAnalysisReport;
import com.opspilot.ai.domain.IncidentEvidence;
import com.opspilot.ai.domain.RecommendedAction;
import com.opspilot.ai.domain.RootCauseCandidate;
import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.metrics.domain.MetricsAvailabilityStatus;
import com.opspilot.metrics.domain.ResourceMetrics;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "opspilot.ai", name = "provider", havingValue = "stub", matchIfMissing = true)
public class StubAiIncidentAnalysisAdapter implements AiIncidentAnalysisPort {

    private static final double HIGH_USAGE_PERCENT = 85.0;

    @Override
    public IncidentAnalysisReport analyze(IncidentAnalysisContext context) {
        List<IncidentEvidence> evidence = new ArrayList<>();
        evidence.add(targetEvidence(context));
        addEventEvidence(context, evidence);
        addMetricEvidence(context.metrics(), evidence);
        addTopologyEvidence(context, evidence);

        ResourceStatus severity = determineSeverity(context, evidence);
        List<RootCauseCandidate> candidates = rootCauseCandidates(context, evidence);

        return new IncidentAnalysisReport(
                UUID.randomUUID(),
                context.clusterId(),
                context.target().namespace(),
                context.target().kind(),
                context.target().name(),
                severity,
                summary(context, severity),
                impactScope(context),
                candidates,
                evidence,
                recommendations(context, severity),
                nextChecks(context),
                "stub",
                "stub-rule-engine",
                null,
                AiTokenUsage.zero(),
                0L,
                Instant.now()
        );
    }

    private IncidentEvidence targetEvidence(IncidentAnalysisContext context) {
        return new IncidentEvidence(
                "target-1",
                "target",
                context.target().kind() + " 상태",
                "대상 상태 메시지: " + context.target().message(),
                context.target().status(),
                context.target().lastUpdatedAt()
        );
    }

    private void addEventEvidence(IncidentAnalysisContext context, List<IncidentEvidence> evidence) {
        int index = 1;

        for (EventSummary event : context.events()) {
            evidence.add(new IncidentEvidence(
                    "event-" + index,
                    "event",
                    "Kubernetes Event: " + event.reason(),
                    "Kubernetes Event 원문: " + event.message(),
                    event.status(),
                    event.lastUpdatedAt()
            ));
            index += 1;
        }
    }

    private void addMetricEvidence(ResourceMetrics metrics, List<IncidentEvidence> evidence) {
        if (metrics == null) {
            return;
        }

        if (metrics.status() == MetricsAvailabilityStatus.UNAVAILABLE) {
            evidence.add(new IncidentEvidence(
                    "metric-1",
                    "metric",
                    "Prometheus metric 사용 불가",
                    metrics.reason(),
                    ResourceStatus.UNKNOWN,
                    metrics.collectedAt()
            ));
            return;
        }

        evidence.add(new IncidentEvidence(
                "metric-1",
                "metric",
                "CPU 사용률",
                "request 대비 " + formatPercent(metrics.summary().cpuUsagePercent()),
                metricStatus(metrics.summary().cpuUsagePercent()),
                metrics.collectedAt()
        ));
        evidence.add(new IncidentEvidence(
                "metric-2",
                "metric",
                "Memory 사용률",
                "request 대비 " + formatPercent(metrics.summary().memoryUsagePercent()),
                metricStatus(metrics.summary().memoryUsagePercent()),
                metrics.collectedAt()
        ));
    }

    private void addTopologyEvidence(IncidentAnalysisContext context, List<IncidentEvidence> evidence) {
        if (context.topology() == null) {
            return;
        }

        evidence.add(new IncidentEvidence(
                "topology-1",
                "topology",
                "관련 객체 그래프",
                "%d개 관련 노드와 %d개 관계를 수집했습니다.".formatted(
                        context.topology().nodes().size(),
                        context.topology().edges().size()
                ),
                ResourceStatus.HEALTHY,
                context.topology().collectedAt()
        ));
    }

    private ResourceStatus determineSeverity(IncidentAnalysisContext context, List<IncidentEvidence> evidence) {
        if (context.target().status() == ResourceStatus.CRITICAL
                || evidence.stream().anyMatch(item -> item.status() == ResourceStatus.CRITICAL)) {
            return ResourceStatus.CRITICAL;
        }

        if (context.target().status() == ResourceStatus.WARNING
                || evidence.stream().anyMatch(item -> item.status() == ResourceStatus.WARNING)) {
            return ResourceStatus.WARNING;
        }

        if (context.target().status() == ResourceStatus.UNKNOWN) {
            return ResourceStatus.UNKNOWN;
        }

        return ResourceStatus.HEALTHY;
    }

    private List<RootCauseCandidate> rootCauseCandidates(
            IncidentAnalysisContext context,
            List<IncidentEvidence> evidence
    ) {
        List<String> allEvidenceIds = evidence.stream().map(IncidentEvidence::id).toList();
        List<RootCauseCandidate> candidates = new ArrayList<>();
        String reason = context.target().reason();

        if ("CrashLoopBackOff".equals(reason)) {
            candidates.add(new RootCauseCandidate(
                    "컨테이너 프로세스가 시작 직후 반복적으로 실패하는 후보",
                    0.78,
                    matchingEvidenceIds(evidence, "target", "event", "metric")
            ));
        } else if ("OOMKilled".equals(reason)) {
            candidates.add(new RootCauseCandidate(
                    "Memory pressure 또는 memory limit 부족 가능성",
                    0.82,
                    matchingEvidenceIds(evidence, "target", "metric")
            ));
        } else if ("UnavailableReplicas".equals(reason) || "Unhealthy".equals(reason)) {
            candidates.add(new RootCauseCandidate(
                    "워크로드 health check 또는 rollout 가용성 문제 가능성",
                    0.72,
                    matchingEvidenceIds(evidence, "target", "event", "topology")
            ));
        }

        if (hasHighMetricUsage(context.metrics())) {
            candidates.add(new RootCauseCandidate(
                    "CPU 또는 Memory 리소스 포화가 증상에 영향을 줄 가능성",
                    0.68,
                    matchingEvidenceIds(evidence, "metric")
            ));
        }

        if (candidates.isEmpty()) {
            candidates.add(new RootCauseCandidate(
                    "수집된 context에서 강한 장애 신호가 발견되지 않았습니다",
                    0.54,
                    allEvidenceIds
            ));
        }

        return candidates;
    }

    private List<RecommendedAction> recommendations(IncidentAnalysisContext context, ResourceStatus severity) {
        List<RecommendedAction> actions = new ArrayList<>();
        actions.add(new RecommendedAction(
                "CHECK_EVENTS_AND_LOGS",
                "low",
                "대상 리소스의 최근 Kubernetes Event와 application log를 확인하세요."
        ));

        if (context.metrics() == null || context.metrics().status() == MetricsAvailabilityStatus.UNAVAILABLE) {
            actions.add(new RecommendedAction(
                    "CHECK_PROMETHEUS_CONNECTION",
                    "low",
                    "Prometheus metric을 사용할 수 없어 metric 기반 근거가 부족합니다."
            ));
        }

        if (severity == ResourceStatus.WARNING || severity == ResourceStatus.CRITICAL) {
            actions.add(new RecommendedAction(
                    "REVIEW_ROLLOUT_OR_RESTART",
                    "medium",
                    "배포 직후 발생했다면 restart/rollback 전에 최근 rollout history를 비교하세요."
            ));
        }

        return actions;
    }

    private List<String> nextChecks(IncidentAnalysisContext context) {
        List<String> checks = new ArrayList<>();
        checks.add("최근 배포 이후 증상이 시작됐는지 확인하세요.");
        checks.add("Event 발생 시각 전후의 application log를 확인하세요.");

        if (context.metrics() == null || context.metrics().status() == MetricsAvailabilityStatus.UNAVAILABLE) {
            checks.add("Prometheus port-forward를 실행한 뒤 metric 근거를 포함해 다시 분석하세요.");
        }

        if (context.topology() != null && !context.topology().edges().isEmpty()) {
            checks.add("Topology에서 관련 Service, Pod, ConfigMap, PVC, Node, Kafka 의존성을 확인하세요.");
        }

        return checks;
    }

    private String summary(IncidentAnalysisContext context, ResourceStatus severity) {
        if (severity == ResourceStatus.HEALTHY) {
            return "%s %s/%s는 현재 정상입니다. 수집된 context에서 강한 장애 신호가 발견되지 않았습니다."
                    .formatted(context.target().kind(), context.target().namespace(), context.target().name());
        }

        return "%s %s/%s는 현재 %s 상태입니다. 주요 원인 후보는 %s입니다. 확정 원인이 아니라 수집된 근거 기반 후보로 보세요."
                .formatted(
                        context.target().kind(),
                        context.target().namespace(),
                        context.target().name(),
                        statusLabel(severity),
                        context.target().reason()
                );
    }

    private List<String> impactScope(IncidentAnalysisContext context) {
        List<String> scope = new ArrayList<>();
        if (context.target().namespace() == null) {
            scope.add(context.target().kind() + "/" + context.target().name());
        } else {
            scope.add(context.target().namespace() + "/" + context.target().kind() + "/" + context.target().name());
        }

        if (context.topology() != null) {
            context.topology().nodes().stream()
                    .filter(node -> node.status() != ResourceStatus.HEALTHY)
                    .limit(5)
                    .map(node -> node.namespace() == null
                            ? node.kind() + "/" + node.name()
                            : node.namespace() + "/" + node.kind() + "/" + node.name())
                    .forEach(scope::add);
        }

        return scope.stream().distinct().toList();
    }

    private boolean hasHighMetricUsage(ResourceMetrics metrics) {
        if (metrics == null || metrics.status() == MetricsAvailabilityStatus.UNAVAILABLE) {
            return false;
        }

        return isHigh(metrics.summary().cpuUsagePercent()) || isHigh(metrics.summary().memoryUsagePercent());
    }

    private boolean isHigh(Double usagePercent) {
        return usagePercent != null && usagePercent >= HIGH_USAGE_PERCENT;
    }

    private ResourceStatus metricStatus(Double usagePercent) {
        if (usagePercent == null) {
            return ResourceStatus.UNKNOWN;
        }

        return usagePercent >= HIGH_USAGE_PERCENT ? ResourceStatus.WARNING : ResourceStatus.HEALTHY;
    }

    private String formatPercent(Double usagePercent) {
        return usagePercent == null ? "알 수 없음" : "%.1f%%".formatted(usagePercent);
    }

    private String statusLabel(ResourceStatus status) {
        return switch (status) {
            case HEALTHY -> "정상";
            case WARNING -> "경고";
            case CRITICAL -> "심각";
            case UNKNOWN -> "알 수 없음";
        };
    }

    private List<String> matchingEvidenceIds(List<IncidentEvidence> evidence, String... types) {
        List<String> expectedTypes = List.of(types);

        return evidence.stream()
                .filter(item -> expectedTypes.contains(item.type()))
                .map(IncidentEvidence::id)
                .toList();
    }
}
