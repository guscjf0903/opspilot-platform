package com.opspilot.topology.application;

import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.NodeSummary;
import com.opspilot.kubernetes.domain.PodSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.topology.domain.KubernetesTopologySnapshot;
import com.opspilot.topology.domain.KubernetesTopologySnapshot.Relation;
import com.opspilot.topology.domain.KubernetesTopologySnapshot.Resource;
import com.opspilot.topology.domain.TopologyEdge;
import com.opspilot.topology.domain.TopologyGraph;
import com.opspilot.topology.domain.TopologyNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TopologyGraphBuilder {

    private static final String EDGE_CONSUMES_FROM = "consumes_from";
    private static final String EDGE_MEMBER_OF = "member_of";
    private static final String EDGE_OWNS = "owns";
    private static final String EDGE_PRODUCES_TO = "produces_to";
    private static final String EDGE_ROUTES_TO = "routes_to";
    private static final String EDGE_SELECTS = "selects";
    private static final String KAFKA_CONSUMER_GROUP = "KafkaConsumerGroup";
    private static final String KAFKA_TOPIC = "KafkaTopic";
    private static final String LABEL_ENDPOINT_SLICE_SERVICE_NAME = "kubernetes.io/service-name";
    private static final Set<String> TRAVERSAL_TERMINAL_KINDS = Set.of("Node", "PersistentVolume");
    private static final String ANNOTATION_KAFKA_DIRECTION = "opspilot.io/kafka-direction";
    private static final String ANNOTATION_KAFKA_CONSUMER_GROUP = "opspilot.io/kafka-consumer-group";
    private static final String ANNOTATION_KAFKA_TOPICS = "opspilot.io/kafka-topics";

    public TopologyGraph build(
            String clusterId,
            KubernetesTopologySnapshot snapshot,
            List<DeploymentSummary> deployments,
            List<PodSummary> pods,
            List<NodeSummary> nodes,
            String rootKind,
            String rootName
    ) {
        Map<String, TopologyNode> graphNodes = new LinkedHashMap<>();
        Set<TopologyEdge> graphEdges = new LinkedHashSet<>();
        Map<String, DeploymentSummary> deploymentsByName = indexByName(deployments, DeploymentSummary::name);
        Map<String, PodSummary> podsByName = indexByName(pods, PodSummary::name);
        Map<String, NodeSummary> nodesByName = indexByName(nodes, NodeSummary::name);

        snapshot.resources().forEach(resource -> graphNodes.put(
                nodeId(resource.kind(), resource.namespace(), resource.name()),
                toNode(resource, deploymentsByName, podsByName, nodesByName)
        ));
        snapshot.resources().forEach(resource -> addResourceEdges(resource, snapshot.resources(), graphNodes, graphEdges));
        snapshot.resources().stream()
                .filter(resource -> "Deployment".equals(resource.kind()))
                .forEach(resource -> addKafkaDependencies(resource, graphNodes, graphEdges));

        String rootNodeId = resolveRootNodeId(snapshot.namespace(), rootKind, rootName, graphNodes);
        if (rootNodeId != null) {
            retainConnectedComponent(rootNodeId, graphNodes, graphEdges);
        }

        return new TopologyGraph(
                clusterId,
                snapshot.namespace(),
                rootNodeId,
                snapshot.collectedAt(),
                graphNodes.values().stream().sorted(nodeComparator()).toList(),
                graphEdges.stream().sorted(edgeComparator()).toList()
        );
    }

    private void addResourceEdges(
            Resource resource,
            List<Resource> resources,
            Map<String, TopologyNode> nodes,
            Set<TopologyEdge> edges
    ) {
        String source = nodeId(resource.kind(), resource.namespace(), resource.name());
        resource.ownerReferences().forEach(owner -> addEdgeIfPresent(
                nodes,
                edges,
                nodeId(owner.kind(), resource.namespace(), owner.name()),
                source,
                EDGE_OWNS
        ));
        resource.relations().forEach(relation -> addRelationEdge(source, relation, nodes, edges));

        if ("Service".equals(resource.kind())) {
            addServiceEdges(resource, resources, source, nodes, edges);
        }

    }

    private void addServiceEdges(
            Resource service,
            List<Resource> resources,
            String serviceId,
            Map<String, TopologyNode> nodes,
            Set<TopologyEdge> edges
    ) {
        resources.stream()
                .filter(resource -> "Pod".equals(resource.kind()))
                .filter(pod -> labelsMatch(service.selector(), pod.labels()))
                .forEach(pod -> addEdgeIfPresent(
                        nodes,
                        edges,
                        serviceId,
                        nodeId(pod.kind(), pod.namespace(), pod.name()),
                        EDGE_SELECTS
                ));
        resources.stream()
                .filter(resource -> "EndpointSlice".equals(resource.kind()))
                .filter(endpointSlice -> service.name().equals(
                        endpointSlice.labels().get(LABEL_ENDPOINT_SLICE_SERVICE_NAME)
                ))
                .forEach(endpointSlice -> addEdgeIfPresent(
                        nodes,
                        edges,
                        serviceId,
                        nodeId(endpointSlice.kind(), endpointSlice.namespace(), endpointSlice.name()),
                        EDGE_ROUTES_TO
                ));
    }

    private void addRelationEdge(
            String source,
            Relation relation,
            Map<String, TopologyNode> nodes,
            Set<TopologyEdge> edges
    ) {
        addEdgeIfPresent(
                nodes,
                edges,
                source,
                nodeId(relation.targetKind(), relation.targetNamespace(), relation.targetName()),
                relation.type()
        );
    }

    private void addKafkaDependencies(
            Resource deployment,
            Map<String, TopologyNode> nodes,
            Set<TopologyEdge> edges
    ) {
        String direction = deployment.annotations().get(ANNOTATION_KAFKA_DIRECTION);
        List<String> topics = commaSeparatedValues(deployment.annotations().get(ANNOTATION_KAFKA_TOPICS));
        if (topics.isEmpty()) {
            return;
        }

        String deploymentId = nodeId(deployment.kind(), deployment.namespace(), deployment.name());
        for (String topic : topics) {
            String topicId = nodeId(KAFKA_TOPIC, null, topic);
            nodes.putIfAbsent(topicId, externalNode(KAFKA_TOPIC, topic));
            if ("produce".equalsIgnoreCase(direction)) {
                edges.add(new TopologyEdge(deploymentId, topicId, EDGE_PRODUCES_TO));
            }
            if ("consume".equalsIgnoreCase(direction)) {
                addKafkaConsumerGroup(deployment, topicId, nodes, edges);
            }
        }
    }

    private void addKafkaConsumerGroup(
            Resource deployment,
            String topicId,
            Map<String, TopologyNode> nodes,
            Set<TopologyEdge> edges
    ) {
        String consumerGroup = deployment.annotations().get(ANNOTATION_KAFKA_CONSUMER_GROUP);
        if (consumerGroup == null || consumerGroup.isBlank()) {
            return;
        }

        String groupId = nodeId(KAFKA_CONSUMER_GROUP, null, consumerGroup);
        nodes.putIfAbsent(groupId, externalNode(KAFKA_CONSUMER_GROUP, consumerGroup));
        edges.add(new TopologyEdge(topicId, groupId, EDGE_CONSUMES_FROM));
        edges.add(new TopologyEdge(
                groupId,
                nodeId(deployment.kind(), deployment.namespace(), deployment.name()),
                EDGE_MEMBER_OF
        ));
    }

    private TopologyNode toNode(
            Resource resource,
            Map<String, DeploymentSummary> deployments,
            Map<String, PodSummary> pods,
            Map<String, NodeSummary> nodes
    ) {
        if ("Deployment".equals(resource.kind())) {
            DeploymentSummary summary = deployments.get(resource.name());
            if (summary != null) {
                return node(resource, summary.status(), summary.reason(), summary.message());
            }
        }

        if ("Pod".equals(resource.kind())) {
            PodSummary summary = pods.get(resource.name());
            if (summary != null) {
                return node(resource, summary.status(), summary.reason(), summary.message());
            }
        }

        if ("Node".equals(resource.kind())) {
            NodeSummary summary = nodes.get(resource.name());
            if (summary != null) {
                return node(resource, summary.status(), summary.reason(), summary.message());
            }
        }

        return node(resource, ResourceStatus.HEALTHY, "Healthy", "Resource metadata is available");
    }

    private TopologyNode node(Resource resource, ResourceStatus status, String reason, String message) {
        return new TopologyNode(
                nodeId(resource.kind(), resource.namespace(), resource.name()),
                resource.kind(),
                resource.namespace(),
                resource.name(),
                status,
                reason,
                message
        );
    }

    private TopologyNode externalNode(String kind, String name) {
        return new TopologyNode(
                nodeId(kind, null, name),
                kind,
                null,
                name,
                ResourceStatus.UNKNOWN,
                "MetadataOnly",
                "Dependency inferred from workload annotation"
        );
    }

    private void addEdgeIfPresent(
            Map<String, TopologyNode> nodes,
            Set<TopologyEdge> edges,
            String source,
            String target,
            String type
    ) {
        if (nodes.containsKey(source) && nodes.containsKey(target)) {
            edges.add(new TopologyEdge(source, target, type));
        }
    }

    private String resolveRootNodeId(
            String namespace,
            String rootKind,
            String rootName,
            Map<String, TopologyNode> nodes
    ) {
        if (rootKind == null || rootName == null) {
            return null;
        }

        return nodes.values().stream()
                .filter(node -> node.kind().equalsIgnoreCase(rootKind))
                .filter(node -> node.name().equals(rootName))
                .filter(node -> node.namespace() == null || node.namespace().equals(namespace))
                .map(TopologyNode::id)
                .findFirst()
                .orElseThrow(() -> new TopologyResourceNotFoundException(rootKind, rootName));
    }

    private void retainConnectedComponent(
            String rootNodeId,
            Map<String, TopologyNode> nodes,
            Set<TopologyEdge> edges
    ) {
        Set<String> connectedNodeIds = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.add(rootNodeId);

        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            if (!connectedNodeIds.add(current)) {
                continue;
            }
            TopologyNode currentNode = nodes.get(current);
            if (!rootNodeId.equals(current)
                    && currentNode != null
                    && TRAVERSAL_TERMINAL_KINDS.contains(currentNode.kind())) {
                continue;
            }

            edges.stream()
                    .filter(edge -> edge.source().equals(current) || edge.target().equals(current))
                    .map(edge -> edge.source().equals(current) ? edge.target() : edge.source())
                    .filter(nodeId -> !connectedNodeIds.contains(nodeId))
                    .forEach(pending::addLast);
        }

        nodes.keySet().retainAll(connectedNodeIds);
        edges.removeIf(edge -> !nodes.containsKey(edge.source()) || !nodes.containsKey(edge.target()));
    }

    private boolean labelsMatch(Map<String, String> selector, Map<String, String> labels) {
        return !selector.isEmpty() && selector.entrySet().stream()
                .allMatch(entry -> entry.getValue().equals(labels.get(entry.getKey())));
    }

    private List<String> commaSeparatedValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private <T> Map<String, T> indexByName(Collection<T> values, Function<T, String> nameExtractor) {
        return values.stream().collect(Collectors.toMap(nameExtractor, Function.identity()));
    }

    private Comparator<TopologyNode> nodeComparator() {
        return Comparator.comparing(TopologyNode::kind).thenComparing(TopologyNode::name);
    }

    private Comparator<TopologyEdge> edgeComparator() {
        return Comparator.comparing(TopologyEdge::source)
                .thenComparing(TopologyEdge::target)
                .thenComparing(TopologyEdge::type);
    }

    private String nodeId(String kind, String namespace, String name) {
        List<String> segments = new ArrayList<>();
        segments.add(kind.toLowerCase(Locale.ROOT));
        if (namespace != null && !namespace.isBlank()) {
            segments.add(namespace);
        }
        segments.add(name);
        return String.join(":", segments);
    }
}
