package com.opspilot.kubernetes.adapter.out.fabric8;

import com.opspilot.kubernetes.application.KubernetesApiException;
import com.opspilot.kubernetes.application.port.out.KubernetesInventoryPort;
import com.opspilot.kubernetes.domain.DeploymentStatusSnapshot;
import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.EventStatusSnapshot;
import com.opspilot.kubernetes.domain.EventSummary;
import com.opspilot.kubernetes.domain.KubernetesResourceStatusEvaluator;
import com.opspilot.kubernetes.domain.NamespaceStatusSnapshot;
import com.opspilot.kubernetes.domain.NamespaceSummary;
import com.opspilot.kubernetes.domain.NodeStatusSnapshot;
import com.opspilot.kubernetes.domain.NodeSummary;
import com.opspilot.kubernetes.domain.PodStatusSnapshot;
import com.opspilot.kubernetes.domain.PodSummary;
import com.opspilot.kubernetes.domain.ResourceHealth;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeCondition;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Fabric8KubernetesInventoryAdapter implements KubernetesInventoryPort {

    private static final String KIND_DEPLOYMENT = "Deployment";
    private static final String KIND_EVENT = "Event";
    private static final String KIND_NAMESPACE = "Namespace";
    private static final String KIND_NODE = "Node";
    private static final String KIND_POD = "Pod";

    private final KubernetesClient kubernetesClient;
    private final KubernetesResourceStatusEvaluator statusEvaluator;

    @Override
    public boolean isReachable() {
        try {
            return kubernetesClient.getKubernetesVersion() != null;
        } catch (KubernetesClientException exception) {
            return false;
        }
    }

    @Override
    public String getApiServerUrl() {
        return kubernetesClient.getConfiguration().getMasterUrl();
    }

    @Override
    public List<NamespaceSummary> getNamespaces() {
        return execute("list namespaces", () -> kubernetesClient.namespaces()
                .list()
                .getItems()
                .stream()
                .map(this::toNamespaceSummary)
                .sorted(Comparator.comparing(NamespaceSummary::name))
                .toList());
    }

    @Override
    public List<DeploymentSummary> getDeployments(String namespace) {
        return execute("list deployments in namespace " + namespace, () -> kubernetesClient.apps()
                .deployments()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .map(this::toDeploymentSummary)
                .sorted(Comparator.comparing(DeploymentSummary::name))
                .toList());
    }

    @Override
    public List<PodSummary> getPods(String namespace) {
        return execute("list pods in namespace " + namespace, () -> kubernetesClient.pods()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .map(this::toPodSummary)
                .sorted(Comparator.comparing(PodSummary::name))
                .toList());
    }

    @Override
    public List<EventSummary> getEvents(String namespace) {
        return execute("list events in namespace " + namespace, () -> kubernetesClient.v1()
                .events()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .map(this::toEventSummary)
                .sorted(Comparator.comparing(
                        EventSummary::lastUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList());
    }

    @Override
    public List<NodeSummary> getNodes() {
        return execute("list nodes", () -> kubernetesClient.nodes()
                .list()
                .getItems()
                .stream()
                .map(this::toNodeSummary)
                .sorted(Comparator.comparing(NodeSummary::name))
                .toList());
    }

    private NamespaceSummary toNamespaceSummary(Namespace namespace) {
        ResourceHealth health = statusEvaluator.evaluateNamespace(new NamespaceStatusSnapshot(
                namespace.getStatus() == null ? null : namespace.getStatus().getPhase()
        ));

        return new NamespaceSummary(
                KIND_NAMESPACE,
                namespace.getMetadata().getName(),
                health.status(),
                health.reason(),
                health.message(),
                getObservationTimestamp()
        );
    }

    private DeploymentSummary toDeploymentSummary(Deployment deployment) {
        int desiredReplicas = deployment.getSpec() == null
                ? deployment.getStatus() == null ? 0 : valueOrZero(deployment.getStatus().getReplicas())
                : valueOrZero(deployment.getSpec().getReplicas());
        int availableReplicas = deployment.getStatus() == null
                ? 0
                : valueOrZero(deployment.getStatus().getAvailableReplicas());
        int unavailableReplicas = deployment.getStatus() == null
                ? 0
                : valueOrZero(deployment.getStatus().getUnavailableReplicas());
        ResourceHealth health = statusEvaluator.evaluateDeployment(new DeploymentStatusSnapshot(
                desiredReplicas,
                availableReplicas,
                unavailableReplicas
        ));

        return new DeploymentSummary(
                KIND_DEPLOYMENT,
                deployment.getMetadata().getNamespace(),
                deployment.getMetadata().getName(),
                health.status(),
                health.reason(),
                health.message(),
                getObservationTimestamp(),
                desiredReplicas,
                availableReplicas,
                deployment.getStatus() == null ? 0 : valueOrZero(deployment.getStatus().getReadyReplicas()),
                deployment.getStatus() == null ? 0 : valueOrZero(deployment.getStatus().getUpdatedReplicas())
        );
    }

    private PodSummary toPodSummary(Pod pod) {
        List<ContainerStatus> containerStatuses = getContainerStatuses(pod);
        int restartCount = containerStatuses.stream()
                .map(ContainerStatus::getRestartCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        ResourceHealth health = statusEvaluator.evaluatePod(new PodStatusSnapshot(
                pod.getStatus() == null ? null : pod.getStatus().getPhase(),
                getWaitingReasons(containerStatuses),
                getCurrentTerminatedReasons(containerStatuses),
                getPreviousTerminatedReasons(containerStatuses),
                restartCount
        ));

        return new PodSummary(
                KIND_POD,
                pod.getMetadata().getNamespace(),
                pod.getMetadata().getName(),
                health.status(),
                health.reason(),
                health.message(),
                getObservationTimestamp(),
                pod.getStatus() == null ? null : pod.getStatus().getPhase(),
                pod.getSpec() == null ? null : pod.getSpec().getNodeName(),
                restartCount,
                pod.getSpec() == null || pod.getSpec().getContainers() == null
                        ? List.of()
                        : pod.getSpec().getContainers().stream().map(container -> container.getImage()).toList()
        );
    }

    private EventSummary toEventSummary(Event event) {
        ResourceHealth health = statusEvaluator.evaluateEvent(new EventStatusSnapshot(
                event.getType(),
                event.getReason(),
                event.getMessage()
        ));

        return new EventSummary(
                KIND_EVENT,
                event.getMetadata().getNamespace(),
                event.getMetadata().getName(),
                health.status(),
                health.reason(),
                health.message(),
                firstTimestamp(event.getLastTimestamp(), event.getFirstTimestamp(), event.getMetadata().getCreationTimestamp()),
                event.getType(),
                event.getInvolvedObject() == null ? null : event.getInvolvedObject().getKind(),
                event.getInvolvedObject() == null ? null : event.getInvolvedObject().getName(),
                valueOrZero(event.getCount())
        );
    }

    private NodeSummary toNodeSummary(Node node) {
        List<NodeCondition> nodeConditions = node.getStatus() == null || node.getStatus().getConditions() == null
                ? List.of()
                : node.getStatus().getConditions();
        Map<String, String> conditions = nodeConditions.stream()
                .collect(Collectors.toMap(NodeCondition::getType, NodeCondition::getStatus, (first, second) -> second));
        ResourceHealth health = statusEvaluator.evaluateNode(new NodeStatusSnapshot(conditions));

        return new NodeSummary(
                KIND_NODE,
                node.getMetadata().getName(),
                health.status(),
                health.reason(),
                health.message(),
                getObservationTimestamp(),
                node.getSpec() != null && Boolean.TRUE.equals(node.getSpec().getUnschedulable()),
                node.getStatus() == null || node.getStatus().getNodeInfo() == null
                        ? null
                        : node.getStatus().getNodeInfo().getKubeletVersion()
        );
    }

    private List<ContainerStatus> getContainerStatuses(Pod pod) {
        if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
            return List.of();
        }

        return pod.getStatus().getContainerStatuses();
    }

    private List<String> getWaitingReasons(List<ContainerStatus> statuses) {
        return statuses.stream()
                .filter(status -> status.getState() != null && status.getState().getWaiting() != null)
                .map(status -> status.getState().getWaiting().getReason())
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> getCurrentTerminatedReasons(List<ContainerStatus> statuses) {
        return getTerminatedReasons(statuses, false);
    }

    private List<String> getPreviousTerminatedReasons(List<ContainerStatus> statuses) {
        return getTerminatedReasons(statuses, true);
    }

    private List<String> getTerminatedReasons(List<ContainerStatus> statuses, boolean previousState) {
        Set<String> reasons = new LinkedHashSet<>();
        statuses.forEach(status -> {
            if (previousState) {
                addTerminatedReason(reasons, status.getLastState());
            } else {
                addTerminatedReason(reasons, status.getState());
            }
        });

        return new ArrayList<>(reasons);
    }

    private void addTerminatedReason(Set<String> reasons, io.fabric8.kubernetes.api.model.ContainerState state) {
        if (state != null && state.getTerminated() != null && state.getTerminated().getReason() != null) {
            reasons.add(state.getTerminated().getReason());
        }
    }

    private Instant getObservationTimestamp() {
        return Instant.now();
    }

    private Instant firstTimestamp(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return Instant.parse(value);
            }
        }

        return null;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private <T> T execute(String operation, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (KubernetesClientException exception) {
            throw new KubernetesApiException(operation, exception);
        }
    }
}
