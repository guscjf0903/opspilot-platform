package com.opspilot.topology.adapter.out.fabric8;

import static org.assertj.core.api.Assertions.assertThat;

import com.opspilot.topology.domain.KubernetesTopologySnapshot;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.NodeBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@EnableKubernetesMockClient(crud = true)
class Fabric8KubernetesTopologyInventoryAdapterTests {

    private KubernetesClient client;
    private Fabric8KubernetesTopologyInventoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new Fabric8KubernetesTopologyInventoryAdapter(client);
    }

    @Test
    void readsMetadataRelationsWithoutExposingSecretValuesOrUnrelatedAnnotations() {
        client.apps().deployments().inNamespace("sample-app").resource(new DeploymentBuilder()
                .withNewMetadata()
                .withName("order-producer")
                .withNamespace("sample-app")
                .withAnnotations(Map.of(
                        "opspilot.io/kafka-direction", "produce",
                        "opspilot.io/kafka-topics", "orders.created",
                        "unsafe.example/value", "do-not-return"
                ))
                .endMetadata()
                .build()).create();
        client.configMaps().inNamespace("sample-app").resource(new ConfigMapBuilder()
                .withNewMetadata().withName("worker-config").withNamespace("sample-app").endMetadata()
                .addToData("mode", "demo")
                .build()).create();
        client.secrets().inNamespace("sample-app").resource(new SecretBuilder()
                .withNewMetadata().withName("worker-secret").withNamespace("sample-app").endMetadata()
                .addToData("password", "do-not-return")
                .build()).create();
        client.nodes().resource(new NodeBuilder()
                .withNewMetadata().withName("demo-control-plane").endMetadata()
                .build()).create();
        client.pods().inNamespace("sample-app").resource(new PodBuilder()
                .withNewMetadata().withName("worker-1").withNamespace("sample-app").endMetadata()
                .withNewSpec()
                .withNodeName("demo-control-plane")
                .addNewVolume()
                .withName("config")
                .withNewConfigMap().withName("worker-config").endConfigMap()
                .endVolume()
                .addNewVolume()
                .withName("secret")
                .withNewSecret().withSecretName("worker-secret").endSecret()
                .endVolume()
                .endSpec()
                .build()).create();

        KubernetesTopologySnapshot snapshot = adapter.getTopologySnapshot("sample-app");

        assertThat(snapshot.resources()).filteredOn(resource -> resource.kind().equals("Deployment"))
                .singleElement()
                .satisfies(resource -> assertThat(resource.annotations())
                        .containsOnly(
                                Map.entry("opspilot.io/kafka-direction", "produce"),
                                Map.entry("opspilot.io/kafka-topics", "orders.created")
                        ));
        assertThat(snapshot.resources()).filteredOn(resource -> resource.kind().equals("Secret"))
                .singleElement()
                .satisfies(resource -> {
                    assertThat(resource.name()).isEqualTo("worker-secret");
                    assertThat(resource.annotations()).isEmpty();
                });
        assertThat(snapshot.resources()).filteredOn(resource -> resource.kind().equals("Pod"))
                .singleElement()
                .satisfies(resource -> assertThat(resource.relations()).extracting("targetKind", "targetName", "type")
                        .contains(
                                org.assertj.core.groups.Tuple.tuple("Node", "demo-control-plane", "scheduled_on"),
                                org.assertj.core.groups.Tuple.tuple("ConfigMap", "worker-config", "mounts"),
                                org.assertj.core.groups.Tuple.tuple("Secret", "worker-secret", "mounts")
                        ));
    }
}
