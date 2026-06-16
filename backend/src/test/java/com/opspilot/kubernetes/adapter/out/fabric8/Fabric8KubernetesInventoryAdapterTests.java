package com.opspilot.kubernetes.adapter.out.fabric8;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspilot.kubernetes.domain.KubernetesResourceStatusEvaluator;
import com.opspilot.kubernetes.domain.ResourceStatus;
import io.fabric8.kubernetes.api.model.ContainerStateBuilder;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceStatusBuilder;
import io.fabric8.kubernetes.api.model.NodeBuilder;
import io.fabric8.kubernetes.api.model.NodeConditionBuilder;
import io.fabric8.kubernetes.api.model.NodeStatusBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@EnableKubernetesMockClient(crud = true)
class Fabric8KubernetesInventoryAdapterTests {

    private KubernetesClient client;
    private Fabric8KubernetesInventoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new Fabric8KubernetesInventoryAdapter(client, new KubernetesResourceStatusEvaluator());
    }

    @Test
    void listsNamespacesDeploymentsPodsEventsAndNodes() {
        client.namespaces().resource(new NamespaceBuilder()
                .withNewMetadata().withName("sample-app").endMetadata()
                .withStatus(new NamespaceStatusBuilder().withPhase("Active").build())
                .build()).create();
        client.apps().deployments().inNamespace("sample-app").resource(new DeploymentBuilder()
                .withNewMetadata().withName("payment-api").withNamespace("sample-app").endMetadata()
                .withStatus(new DeploymentStatusBuilder()
                        .withReplicas(2)
                        .withAvailableReplicas(1)
                        .withReadyReplicas(1)
                        .withUpdatedReplicas(2)
                        .withUnavailableReplicas(1)
                        .build())
                .build()).create();
        client.pods().inNamespace("sample-app").resource(new PodBuilder()
                .withNewMetadata().withName("worker-1").withNamespace("sample-app").endMetadata()
                .withSpec(new PodSpecBuilder()
                        .addNewContainer().withName("worker").withImage("busybox:1.37.0").endContainer()
                        .build())
                .withStatus(new PodStatusBuilder()
                        .withPhase("Running")
                        .addToContainerStatuses(new ContainerStatusBuilder()
                                .withName("worker")
                                .withRestartCount(3)
                                .withState(new ContainerStateBuilder()
                                        .withNewWaiting().withReason("CrashLoopBackOff").endWaiting()
                                        .build())
                                .build())
                        .build())
                .build()).create();
        client.v1().events().inNamespace("sample-app").resource(new EventBuilder()
                .withNewMetadata().withName("worker-backoff").withNamespace("sample-app").endMetadata()
                .withType("Warning")
                .withReason("BackOff")
                .withMessage("Back-off restarting failed container")
                .withNewInvolvedObject().withKind("Pod").withName("worker-1").endInvolvedObject()
                .build()).create();
        client.nodes().resource(new NodeBuilder()
                .withNewMetadata().withName("demo-control-plane").endMetadata()
                .withStatus(new NodeStatusBuilder()
                        .addToConditions(new NodeConditionBuilder().withType("Ready").withStatus("True").build())
                        .build())
                .build()).create();

        assertThat(adapter.getNamespaces()).singleElement()
                .satisfies(namespace -> assertThat(namespace.status()).isEqualTo(ResourceStatus.HEALTHY));
        assertThat(adapter.getDeployments("sample-app")).singleElement()
                .satisfies(deployment -> assertThat(deployment.status()).isEqualTo(ResourceStatus.WARNING));
        assertThat(adapter.getPods("sample-app")).singleElement()
                .satisfies(pod -> {
                    assertThat(pod.status()).isEqualTo(ResourceStatus.CRITICAL);
                    assertThat(pod.reason()).isEqualTo("CrashLoopBackOff");
                });
        assertThat(adapter.getEvents("sample-app")).singleElement()
                .satisfies(event -> assertThat(event.status()).isEqualTo(ResourceStatus.WARNING));
        assertThat(adapter.getNodes()).singleElement()
                .satisfies(node -> assertThat(node.status()).isEqualTo(ResourceStatus.HEALTHY));
    }
}
