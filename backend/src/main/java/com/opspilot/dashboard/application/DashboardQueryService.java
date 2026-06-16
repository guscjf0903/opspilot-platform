package com.opspilot.dashboard.application;

import com.opspilot.dashboard.domain.DashboardCounts;
import com.opspilot.dashboard.domain.DashboardExternalMetric;
import com.opspilot.dashboard.domain.DashboardExternalSignal;
import com.opspilot.dashboard.domain.DashboardNamespaceSummary;
import com.opspilot.dashboard.domain.DashboardRestartSummary;
import com.opspilot.dashboard.domain.DashboardSummary;
import com.opspilot.dashboard.domain.DashboardWorkloadSummary;
import com.opspilot.kafka.application.KafkaQueryService;
import com.opspilot.kafka.domain.KafkaConsumerGroupSummary;
import com.opspilot.kafka.domain.KafkaOverview;
import com.opspilot.kubernetes.application.KubernetesInventoryService;
import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.kubernetes.domain.NamespaceSummary;
import com.opspilot.kubernetes.domain.NodeSummary;
import com.opspilot.kubernetes.domain.PodSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.metrics.application.MetricsQueryService;
import com.opspilot.metrics.domain.MetricsAvailabilityStatus;
import com.opspilot.metrics.domain.NodeUsageMetric;
import com.opspilot.metrics.domain.NodeUsageSnapshot;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    private static final Duration RECENT_EVENT_WINDOW = Duration.ofMinutes(30);
    private static final int RECENT_EVENT_LIMIT = 8;
    private static final int RESTART_COUNT_LIMIT = 5;
    private static final int UNHEALTHY_WORKLOAD_LIMIT = 10;

    private final KubernetesInventoryService kubernetesInventoryService;
    private final MetricsQueryService metricsQueryService;
    private final KafkaQueryService kafkaQueryService;
    private final Clock dashboardClock;

    public DashboardSummary getDashboard(String clusterId, String namespace) {
        Instant collectedAt = dashboardClock.instant();
        List<NamespaceSummary> namespaces = kubernetesInventoryService.getNamespaces(clusterId);
        List<NodeSummary> nodes = kubernetesInventoryService.getNodes(clusterId);
        List<NamespaceSummary> targetNamespaces = getTargetNamespaces(namespaces, namespace);
        List<NamespaceInventory> namespaceInventories = targetNamespaces.stream()
                .map(targetNamespace -> getNamespaceInventory(clusterId, targetNamespace))
                .toList();

        List<DeploymentSummary> deployments = namespaceInventories.stream()
                .flatMap(inventory -> inventory.deployments().stream())
                .toList();
        List<PodSummary> pods = namespaceInventories.stream()
                .flatMap(inventory -> inventory.pods().stream())
                .toList();
        List<EventSummary> recentWarningEvents = getRecentWarningEvents(namespaceInventories, collectedAt);
        List<DashboardWorkloadSummary> unhealthyWorkloads = getUnhealthyWorkloads(deployments, pods);
        List<DashboardNamespaceSummary> namespaceSummaries = namespaceInventories.stream()
                .map(this::toNamespaceSummary)
                .sorted(Comparator.comparing(DashboardNamespaceSummary::name))
                .toList();

        return new DashboardSummary(
                getClusterStatus(namespaces, nodes, deployments, pods, recentWarningEvents),
                normalizeNamespace(namespace),
                collectedAt,
                new DashboardCounts(
                        namespaces.size(),
                        nodes.size(),
                        deployments.size(),
                        pods.size(),
                        countWorkloadsWithStatus(deployments, pods, ResourceStatus.CRITICAL),
                        countWorkloadsWithStatus(deployments, pods, ResourceStatus.WARNING),
                        recentWarningEvents.size()
                ),
                unhealthyWorkloads.stream().limit(UNHEALTHY_WORKLOAD_LIMIT).toList(),
                recentWarningEvents.stream().limit(RECENT_EVENT_LIMIT).toList(),
                getRestartCountTopPods(pods),
                namespaceSummaries,
                nodes,
                getNodeUsageSignal(clusterId, nodes),
                getKafkaLagSignal(clusterId),
                DashboardExternalSignal.unavailable("OPENCOST_NOT_CONNECTED")
        );
    }

    private NamespaceInventory getNamespaceInventory(String clusterId, NamespaceSummary namespace) {
        return new NamespaceInventory(
                namespace,
                kubernetesInventoryService.getDeployments(clusterId, namespace.name()),
                kubernetesInventoryService.getPods(clusterId, namespace.name()),
                kubernetesInventoryService.getEvents(clusterId, namespace.name())
        );
    }

    private List<NamespaceSummary> getTargetNamespaces(List<NamespaceSummary> namespaces, String namespace) {
        String normalizedNamespace = normalizeNamespace(namespace);

        if (normalizedNamespace != null) {
            return namespaces.stream()
                    .filter(namespaceSummary -> namespaceSummary.name().equals(normalizedNamespace))
                    .toList();
        }

        return namespaces;
    }

    private String normalizeNamespace(String namespace) {
        return namespace == null || namespace.isBlank() ? null : namespace.trim();
    }

    private List<EventSummary> getRecentWarningEvents(
            List<NamespaceInventory> inventories,
            Instant collectedAt
    ) {
        Instant cutoff = collectedAt.minus(RECENT_EVENT_WINDOW);

        return inventories.stream()
                .flatMap(inventory -> inventory.events().stream())
                .filter(event -> event.status() != ResourceStatus.HEALTHY)
                .filter(event -> event.lastUpdatedAt() != null && !event.lastUpdatedAt().isBefore(cutoff))
                .sorted(Comparator.comparing(
                        EventSummary::lastUpdatedAt,
                        Comparator.reverseOrder()
                ))
                .toList();
    }

    private List<DashboardWorkloadSummary> getUnhealthyWorkloads(
            List<DeploymentSummary> deployments,
            List<PodSummary> pods
    ) {
        List<DashboardWorkloadSummary> workloads = new ArrayList<>();
        deployments.stream()
                .filter(deployment -> deployment.status() != ResourceStatus.HEALTHY)
                .map(deployment -> new DashboardWorkloadSummary(
                        deployment.kind(),
                        deployment.namespace(),
                        deployment.name(),
                        deployment.status(),
                        deployment.reason(),
                        deployment.message()
                ))
                .forEach(workloads::add);
        pods.stream()
                .filter(pod -> pod.status() != ResourceStatus.HEALTHY)
                .map(pod -> new DashboardWorkloadSummary(
                        pod.kind(),
                        pod.namespace(),
                        pod.name(),
                        pod.status(),
                        pod.reason(),
                        pod.message()
                ))
                .forEach(workloads::add);

        return workloads.stream()
                .sorted(Comparator
                        .comparing((DashboardWorkloadSummary workload) -> getStatusRank(workload.status()))
                        .reversed()
                        .thenComparing(DashboardWorkloadSummary::namespace)
                        .thenComparing(DashboardWorkloadSummary::name))
                .toList();
    }

    private List<DashboardRestartSummary> getRestartCountTopPods(List<PodSummary> pods) {
        return pods.stream()
                .filter(pod -> pod.restartCount() > 0)
                .sorted(Comparator.comparing(PodSummary::restartCount).reversed())
                .limit(RESTART_COUNT_LIMIT)
                .map(pod -> new DashboardRestartSummary(
                        pod.namespace(),
                        pod.name(),
                        pod.status(),
                        pod.reason(),
                        pod.restartCount()
                ))
                .toList();
    }

    private DashboardNamespaceSummary toNamespaceSummary(NamespaceInventory inventory) {
        List<ResourceStatus> workloadStatuses = new ArrayList<>();
        workloadStatuses.add(inventory.namespace().status());
        inventory.deployments().stream().map(DeploymentSummary::status).forEach(workloadStatuses::add);
        inventory.pods().stream().map(PodSummary::status).forEach(workloadStatuses::add);

        return new DashboardNamespaceSummary(
                inventory.namespace().name(),
                getMostSevereStatus(workloadStatuses),
                inventory.deployments().size(),
                inventory.pods().size(),
                (int) workloadStatuses.stream().filter(status -> status != ResourceStatus.HEALTHY).count()
        );
    }

    private DashboardExternalSignal getNodeUsageSignal(String clusterId, List<NodeSummary> nodes) {
        NodeUsageSnapshot snapshot = metricsQueryService.getNodeUsage(
                clusterId,
                nodes.stream().map(NodeSummary::name).toList()
        );

        if (snapshot.status() == MetricsAvailabilityStatus.UNAVAILABLE) {
            return DashboardExternalSignal.unavailable(snapshot.reason());
        }

        return DashboardExternalSignal.available(
                snapshot.reason(),
                snapshot.nodes().stream()
                        .map(this::toDashboardExternalMetric)
                        .toList()
        );
    }

    private DashboardExternalSignal getKafkaLagSignal(String clusterId) {
        KafkaOverview overview = kafkaQueryService.getOverview(clusterId);

        if (!overview.available()) {
            return DashboardExternalSignal.unavailable(overview.reason());
        }

        List<DashboardExternalMetric> metrics = kafkaQueryService.getConsumerGroups(clusterId).stream()
                .filter(group -> group.totalLag() > 0)
                .sorted(Comparator.comparing(KafkaConsumerGroupSummary::totalLag).reversed())
                .limit(5)
                .map(group -> DashboardExternalMetric.value(
                        group.groupId(),
                        group.totalLag(),
                        "messages",
                        group.status(),
                        group.reason()
                ))
                .toList();

        return DashboardExternalSignal.available(overview.reason(), metrics);
    }

    private DashboardExternalMetric toDashboardExternalMetric(NodeUsageMetric nodeUsage) {
        return new DashboardExternalMetric(
                nodeUsage.nodeName(),
                nodeUsage.cpuUsageCores(),
                nodeUsage.memoryUsageBytes(),
                nodeUsage.cpuUsagePercent(),
                nodeUsage.memoryUsagePercent()
        );
    }

    private ResourceStatus getClusterStatus(
            List<NamespaceSummary> namespaces,
            List<NodeSummary> nodes,
            List<DeploymentSummary> deployments,
            List<PodSummary> pods,
            List<EventSummary> recentWarningEvents
    ) {
        List<ResourceStatus> statuses = new ArrayList<>();
        namespaces.stream().map(NamespaceSummary::status).forEach(statuses::add);
        nodes.stream().map(NodeSummary::status).forEach(statuses::add);
        deployments.stream().map(DeploymentSummary::status).forEach(statuses::add);
        pods.stream().map(PodSummary::status).forEach(statuses::add);
        recentWarningEvents.stream().map(EventSummary::status).forEach(statuses::add);

        return getMostSevereStatus(statuses);
    }

    private ResourceStatus getMostSevereStatus(List<ResourceStatus> statuses) {
        return statuses.stream()
                .max(Comparator.comparing(this::getStatusRank))
                .orElse(ResourceStatus.UNKNOWN);
    }

    private int countWorkloadsWithStatus(
            List<DeploymentSummary> deployments,
            List<PodSummary> pods,
            ResourceStatus status
    ) {
        return (int) (deployments.stream().filter(deployment -> deployment.status() == status).count()
                + pods.stream().filter(pod -> pod.status() == status).count());
    }

    private int getStatusRank(ResourceStatus status) {
        return switch (status) {
            case CRITICAL -> 3;
            case WARNING -> 2;
            case UNKNOWN -> 1;
            case HEALTHY -> 0;
        };
    }

    private record NamespaceInventory(
            NamespaceSummary namespace,
            List<DeploymentSummary> deployments,
            List<PodSummary> pods,
            List<EventSummary> events
    ) {
    }
}
