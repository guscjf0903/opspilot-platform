package com.opspilot.topology.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspilot.kubernetes.domain.DeploymentSummary;
import com.opspilot.kubernetes.domain.NodeSummary;
import com.opspilot.kubernetes.domain.PodSummary;
import com.opspilot.kubernetes.domain.ResourceStatus;
import com.opspilot.topology.domain.KubernetesTopologySnapshot;
import com.opspilot.topology.domain.KubernetesTopologySnapshot.OwnerReference;
import com.opspilot.topology.domain.KubernetesTopologySnapshot.Relation;
import com.opspilot.topology.domain.KubernetesTopologySnapshot.Resource;
import com.opspilot.topology.domain.TopologyEdge;
import com.opspilot.topology.domain.TopologyGraph;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TopologyGraphBuilderTests {

    private final TopologyGraphBuilder graphBuilder = new TopologyGraphBuilder();

    @Test
    void buildsKubernetesAndKafkaDependencyEdgesAndRetainsResourceHealth() {
        TopologyGraph graph = graphBuilder.build(
                "local",
                snapshot(),
                deployments(),
                pods(),
                nodes(),
                null,
                null
        );

        assertThat(graph.nodes()).extracting("id")
                .contains(
                        "deployment:sample-app:order-producer",
                        "replicaset:sample-app:order-producer-rs",
                        "pod:sample-app:order-producer-pod",
                        "service:sample-app:orders",
                        "endpointslice:sample-app:orders-slice",
                        "node:demo-control-plane",
                        "kafkatopic:orders.created",
                        "kafkaconsumergroup:order-consumer"
                );
        assertThat(graph.edges()).contains(
                edge("deployment:sample-app:order-producer", "replicaset:sample-app:order-producer-rs", "owns"),
                edge("replicaset:sample-app:order-producer-rs", "pod:sample-app:order-producer-pod", "owns"),
                edge("service:sample-app:orders", "pod:sample-app:order-producer-pod", "selects"),
                edge("service:sample-app:orders", "endpointslice:sample-app:orders-slice", "routes_to"),
                edge("endpointslice:sample-app:orders-slice", "pod:sample-app:order-producer-pod", "routes_to"),
                edge("pod:sample-app:order-producer-pod", "node:demo-control-plane", "scheduled_on"),
                edge("deployment:sample-app:order-producer", "kafkatopic:orders.created", "produces_to"),
                edge("kafkatopic:orders.created", "kafkaconsumergroup:order-consumer", "consumes_from"),
                edge("kafkaconsumergroup:order-consumer", "deployment:sample-app:order-consumer", "member_of")
        );
        assertThat(graph.nodes()).filteredOn(node -> node.name().equals("order-producer-pod"))
                .singleElement()
                .satisfies(node -> assertThat(node.status()).isEqualTo(ResourceStatus.CRITICAL));
    }

    @Test
    void returnsOnlyConnectedResourcesWhenRootWorkloadIsProvided() {
        TopologyGraph graph = graphBuilder.build(
                "local",
                snapshot(),
                deployments(),
                pods(),
                nodes(),
                "Deployment",
                "order-producer"
        );

        assertThat(graph.rootNodeId()).isEqualTo("deployment:sample-app:order-producer");
        assertThat(graph.nodes()).extracting("id")
                .contains("kafkatopic:orders.created", "deployment:sample-app:order-consumer")
                .doesNotContain("secret:sample-app:unrelated-secret", "pod:sample-app:unrelated-pod");
    }

    private KubernetesTopologySnapshot snapshot() {
        return new KubernetesTopologySnapshot(
                "sample-app",
                Instant.parse("2026-06-02T00:30:00Z"),
                List.of(
                        resource(
                                "Deployment",
                                "order-producer",
                                Map.of(),
                                Map.of(
                                        "opspilot.io/kafka-direction", "produce",
                                        "opspilot.io/kafka-topics", "orders.created"
                                ),
                                List.of(),
                                List.of()
                        ),
                        resource(
                                "ReplicaSet",
                                "order-producer-rs",
                                Map.of(),
                                Map.of(),
                                List.of(new OwnerReference("Deployment", "order-producer")),
                                List.of()
                        ),
                        resource(
                                "Pod",
                                "order-producer-pod",
                                Map.of("app", "orders"),
                                Map.of(),
                                List.of(new OwnerReference("ReplicaSet", "order-producer-rs")),
                                List.of(new Relation("Node", null, "demo-control-plane", "scheduled_on"))
                        ),
                        resource("Service", "orders", Map.of(), Map.of(), List.of(), List.of(), Map.of("app", "orders")),
                        resource(
                                "EndpointSlice",
                                "orders-slice",
                                Map.of("kubernetes.io/service-name", "orders"),
                                Map.of(),
                                List.of(),
                                List.of(new Relation("Pod", "sample-app", "order-producer-pod", "routes_to"))
                        ),
                        resource(
                                "Deployment",
                                "order-consumer",
                                Map.of(),
                                Map.of(
                                        "opspilot.io/kafka-direction", "consume",
                                        "opspilot.io/kafka-topics", "orders.created",
                                        "opspilot.io/kafka-consumer-group", "order-consumer"
                                ),
                                List.of(),
                                List.of()
                        ),
                        resource(
                                "Pod",
                                "unrelated-pod",
                                Map.of(),
                                Map.of(),
                                List.of(),
                                List.of(new Relation("Node", null, "demo-control-plane", "scheduled_on"))
                        ),
                        resource("Node", null, "demo-control-plane"),
                        resource("Secret", "unrelated-secret")
                )
        );
    }

    private List<DeploymentSummary> deployments() {
        return List.of(
                deployment("order-producer", ResourceStatus.HEALTHY),
                deployment("order-consumer", ResourceStatus.WARNING)
        );
    }

    private DeploymentSummary deployment(String name, ResourceStatus status) {
        return new DeploymentSummary(
                "Deployment",
                "sample-app",
                name,
                status,
                status == ResourceStatus.HEALTHY ? "Healthy" : "UnavailableReplicas",
                "deployment status",
                Instant.parse("2026-06-02T00:30:00Z"),
                1,
                status == ResourceStatus.HEALTHY ? 1 : 0,
                status == ResourceStatus.HEALTHY ? 1 : 0,
                1
        );
    }

    private List<PodSummary> pods() {
        return List.of(new PodSummary(
                "Pod",
                "sample-app",
                "order-producer-pod",
                ResourceStatus.CRITICAL,
                "CrashLoopBackOff",
                "Container is waiting: CrashLoopBackOff",
                Instant.parse("2026-06-02T00:30:00Z"),
                "Running",
                "demo-control-plane",
                3,
                List.of("demo")
        ));
    }

    private List<NodeSummary> nodes() {
        return List.of(new NodeSummary(
                "Node",
                "demo-control-plane",
                ResourceStatus.HEALTHY,
                "Healthy",
                "node status",
                Instant.parse("2026-06-02T00:30:00Z"),
                false,
                "v1.0.0"
        ));
    }

    private Resource resource(String kind, String name) {
        return resource(kind, "sample-app", name);
    }

    private Resource resource(String kind, String namespace, String name) {
        return resource(kind, namespace, name, Map.of(), Map.of(), List.of(), List.of(), Map.of());
    }

    private Resource resource(
            String kind,
            String name,
            Map<String, String> labels,
            Map<String, String> annotations,
            List<OwnerReference> ownerReferences,
            List<Relation> relations
    ) {
        return resource(kind, "sample-app", name, labels, annotations, ownerReferences, relations, Map.of());
    }

    private Resource resource(
            String kind,
            String name,
            Map<String, String> labels,
            Map<String, String> annotations,
            List<OwnerReference> ownerReferences,
            List<Relation> relations,
            Map<String, String> selector
    ) {
        return resource(kind, "sample-app", name, labels, annotations, ownerReferences, relations, selector);
    }

    private Resource resource(
            String kind,
            String namespace,
            String name,
            Map<String, String> labels,
            Map<String, String> annotations,
            List<OwnerReference> ownerReferences,
            List<Relation> relations,
            Map<String, String> selector
    ) {
        return new Resource(kind, namespace, name, labels, annotations, ownerReferences, selector, relations);
    }

    private TopologyEdge edge(String source, String target, String type) {
        return new TopologyEdge(source, target, type);
    }
}
