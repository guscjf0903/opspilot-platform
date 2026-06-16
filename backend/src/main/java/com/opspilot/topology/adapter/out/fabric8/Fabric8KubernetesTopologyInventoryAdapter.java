package com.opspilot.topology.adapter.out.fabric8;

import com.opspilot.kubernetes.application.KubernetesApiException;
import com.opspilot.topology.application.port.out.KubernetesTopologyInventoryPort;
import com.opspilot.topology.domain.KubernetesTopologySnapshot;
import com.opspilot.topology.domain.KubernetesTopologySnapshot.OwnerReference;
import com.opspilot.topology.domain.KubernetesTopologySnapshot.Relation;
import com.opspilot.topology.domain.KubernetesTopologySnapshot.Resource;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.discovery.v1.Endpoint;
import io.fabric8.kubernetes.api.model.discovery.v1.EndpointSlice;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Fabric8KubernetesTopologyInventoryAdapter implements KubernetesTopologyInventoryPort {

    private static final String EDGE_MOUNTS = "mounts";
    private static final String EDGE_ROUTES_TO = "routes_to";
    private static final String EDGE_SCALES = "scales";
    private static final String EDGE_SCHEDULED_ON = "scheduled_on";
    private static final String KAFKA_ANNOTATION_PREFIX = "opspilot.io/kafka-";

    private final KubernetesClient kubernetesClient;

    @Override
    public KubernetesTopologySnapshot getTopologySnapshot(String namespace) {
        return execute("build topology snapshot in namespace " + namespace, () -> {
            List<Resource> resources = new ArrayList<>();
            resources.addAll(getDeployments(namespace));
            resources.addAll(getReplicaSets(namespace));
            resources.addAll(getPods(namespace));
            resources.addAll(getServices(namespace));
            resources.addAll(getEndpointSlices(namespace));
            resources.addAll(getIngresses(namespace));
            resources.addAll(getHorizontalPodAutoscalers(namespace));
            resources.addAll(getConfigMaps(namespace));
            resources.addAll(getSecrets(namespace));
            resources.addAll(getPersistentVolumeClaims(namespace));
            resources.addAll(getPersistentVolumes());
            resources.addAll(getNodes());
            return new KubernetesTopologySnapshot(namespace, Instant.now(), resources);
        });
    }

    private List<Resource> getDeployments(String namespace) {
        return kubernetesClient.apps().deployments().inNamespace(namespace).list().getItems().stream()
                .map(deployment -> resource("Deployment", deployment))
                .toList();
    }

    private List<Resource> getReplicaSets(String namespace) {
        return kubernetesClient.apps().replicaSets().inNamespace(namespace).list().getItems().stream()
                .map(replicaSet -> resource("ReplicaSet", replicaSet))
                .toList();
    }

    private List<Resource> getPods(String namespace) {
        return kubernetesClient.pods().inNamespace(namespace).list().getItems().stream()
                .map(pod -> resource("Pod", pod, Map.of(), getPodRelations(pod)))
                .toList();
    }

    private List<Resource> getServices(String namespace) {
        return kubernetesClient.services().inNamespace(namespace).list().getItems().stream()
                .map(service -> resource(
                        "Service",
                        service,
                        service.getSpec() == null ? Map.of() : safeMap(service.getSpec().getSelector()),
                        List.of()
                ))
                .toList();
    }

    private List<Resource> getEndpointSlices(String namespace) {
        return kubernetesClient.discovery().v1().endpointSlices().inNamespace(namespace).list().getItems().stream()
                .map(endpointSlice -> resource(
                        "EndpointSlice",
                        endpointSlice,
                        Map.of(),
                        getEndpointSliceRelations(endpointSlice)
                ))
                .toList();
    }

    private List<Resource> getIngresses(String namespace) {
        return kubernetesClient.network().v1().ingresses().inNamespace(namespace).list().getItems().stream()
                .map(ingress -> resource("Ingress", ingress, Map.of(), getIngressRelations(ingress)))
                .toList();
    }

    private List<Resource> getHorizontalPodAutoscalers(String namespace) {
        return kubernetesClient.autoscaling().v2().horizontalPodAutoscalers().inNamespace(namespace).list()
                .getItems()
                .stream()
                .map(hpa -> resource("HorizontalPodAutoscaler", hpa, Map.of(), getHpaRelations(hpa)))
                .toList();
    }

    private List<Resource> getConfigMaps(String namespace) {
        return kubernetesClient.configMaps().inNamespace(namespace).list().getItems().stream()
                .map(configMap -> resource("ConfigMap", configMap))
                .toList();
    }

    private List<Resource> getSecrets(String namespace) {
        return kubernetesClient.secrets().inNamespace(namespace).list().getItems().stream()
                .map(secret -> resource("Secret", secret))
                .toList();
    }

    private List<Resource> getPersistentVolumeClaims(String namespace) {
        return kubernetesClient.persistentVolumeClaims().inNamespace(namespace).list().getItems().stream()
                .map(claim -> resource("PersistentVolumeClaim", claim, Map.of(), getPvcRelations(claim)))
                .toList();
    }

    private List<Resource> getPersistentVolumes() {
        return kubernetesClient.persistentVolumes().list().getItems().stream()
                .map(volume -> resource("PersistentVolume", volume))
                .toList();
    }

    private List<Resource> getNodes() {
        return kubernetesClient.nodes().list().getItems().stream()
                .map(node -> resource("Node", node))
                .toList();
    }

    private Resource resource(String kind, HasMetadata metadata) {
        return resource(kind, metadata, Map.of(), List.of());
    }

    private Resource resource(
            String kind,
            HasMetadata resource,
            Map<String, String> selector,
            List<Relation> relations
    ) {
        return new Resource(
                kind,
                resource.getMetadata().getNamespace(),
                resource.getMetadata().getName(),
                safeMap(resource.getMetadata().getLabels()),
                safeAnnotations(resource),
                getOwnerReferences(resource),
                selector,
                relations
        );
    }

    private List<OwnerReference> getOwnerReferences(HasMetadata resource) {
        if (resource.getMetadata().getOwnerReferences() == null) {
            return List.of();
        }

        return resource.getMetadata().getOwnerReferences().stream()
                .map(reference -> new OwnerReference(reference.getKind(), reference.getName()))
                .toList();
    }

    private List<Relation> getPodRelations(Pod pod) {
        Set<Relation> relations = new LinkedHashSet<>();
        if (pod.getSpec() == null) {
            return List.of();
        }

        if (pod.getSpec().getNodeName() != null) {
            relations.add(new Relation("Node", null, pod.getSpec().getNodeName(), EDGE_SCHEDULED_ON));
        }
        safeList(pod.getSpec().getVolumes()).forEach(volume -> addVolumeRelations(pod, volume, relations));
        allContainers(pod).forEach(container -> addContainerRelations(pod, container, relations));
        return List.copyOf(relations);
    }

    private void addVolumeRelations(Pod pod, Volume volume, Set<Relation> relations) {
        if (volume.getConfigMap() != null) {
            relations.add(namespaceRelation("ConfigMap", pod, volume.getConfigMap().getName(), EDGE_MOUNTS));
        }
        if (volume.getSecret() != null) {
            relations.add(namespaceRelation("Secret", pod, volume.getSecret().getSecretName(), EDGE_MOUNTS));
        }
        if (volume.getPersistentVolumeClaim() != null) {
            relations.add(namespaceRelation(
                    "PersistentVolumeClaim",
                    pod,
                    volume.getPersistentVolumeClaim().getClaimName(),
                    EDGE_MOUNTS
            ));
        }
    }

    private void addContainerRelations(Pod pod, Container container, Set<Relation> relations) {
        safeList(container.getEnvFrom()).forEach(source -> addEnvFromRelations(pod, source, relations));
        safeList(container.getEnv()).forEach(envVar -> addEnvVarRelations(pod, envVar, relations));
    }

    private void addEnvFromRelations(Pod pod, EnvFromSource source, Set<Relation> relations) {
        if (source.getConfigMapRef() != null) {
            relations.add(namespaceRelation("ConfigMap", pod, source.getConfigMapRef().getName(), EDGE_MOUNTS));
        }
        if (source.getSecretRef() != null) {
            relations.add(namespaceRelation("Secret", pod, source.getSecretRef().getName(), EDGE_MOUNTS));
        }
    }

    private void addEnvVarRelations(Pod pod, EnvVar envVar, Set<Relation> relations) {
        if (envVar.getValueFrom() == null) {
            return;
        }
        if (envVar.getValueFrom().getConfigMapKeyRef() != null) {
            relations.add(namespaceRelation(
                    "ConfigMap",
                    pod,
                    envVar.getValueFrom().getConfigMapKeyRef().getName(),
                    EDGE_MOUNTS
            ));
        }
        if (envVar.getValueFrom().getSecretKeyRef() != null) {
            relations.add(namespaceRelation(
                    "Secret",
                    pod,
                    envVar.getValueFrom().getSecretKeyRef().getName(),
                    EDGE_MOUNTS
            ));
        }
    }

    private Stream<Container> allContainers(Pod pod) {
        return Stream.concat(
                safeList(pod.getSpec().getInitContainers()).stream(),
                safeList(pod.getSpec().getContainers()).stream()
        );
    }

    private List<Relation> getEndpointSliceRelations(EndpointSlice endpointSlice) {
        return safeList(endpointSlice.getEndpoints()).stream()
                .map(Endpoint::getTargetRef)
                .filter(Objects::nonNull)
                .filter(reference -> reference.getKind() != null && reference.getName() != null)
                .map(reference -> new Relation(
                        reference.getKind(),
                        valueOrDefault(reference.getNamespace(), endpointSlice.getMetadata().getNamespace()),
                        reference.getName(),
                        EDGE_ROUTES_TO
                ))
                .toList();
    }

    private List<Relation> getIngressRelations(Ingress ingress) {
        if (ingress.getSpec() == null) {
            return List.of();
        }

        Set<String> serviceNames = new LinkedHashSet<>();
        addIngressBackendService(serviceNames, ingress.getSpec().getDefaultBackend());
        safeList(ingress.getSpec().getRules()).stream()
                .map(IngressRule::getHttp)
                .filter(Objects::nonNull)
                .flatMap(http -> safeList(http.getPaths()).stream())
                .map(HTTPIngressPath::getBackend)
                .forEach(backend -> addIngressBackendService(serviceNames, backend));
        return serviceNames.stream()
                .map(serviceName -> new Relation(
                        "Service",
                        ingress.getMetadata().getNamespace(),
                        serviceName,
                        EDGE_ROUTES_TO
                ))
                .toList();
    }

    private void addIngressBackendService(Set<String> serviceNames, IngressBackend backend) {
        if (backend != null && backend.getService() != null && backend.getService().getName() != null) {
            serviceNames.add(backend.getService().getName());
        }
    }

    private List<Relation> getHpaRelations(HorizontalPodAutoscaler hpa) {
        if (hpa.getSpec() == null || hpa.getSpec().getScaleTargetRef() == null) {
            return List.of();
        }

        return List.of(new Relation(
                hpa.getSpec().getScaleTargetRef().getKind(),
                hpa.getMetadata().getNamespace(),
                hpa.getSpec().getScaleTargetRef().getName(),
                EDGE_SCALES
        ));
    }

    private List<Relation> getPvcRelations(PersistentVolumeClaim claim) {
        if (claim.getSpec() == null || claim.getSpec().getVolumeName() == null) {
            return List.of();
        }

        return List.of(new Relation("PersistentVolume", null, claim.getSpec().getVolumeName(), EDGE_MOUNTS));
    }

    private Relation namespaceRelation(String kind, Pod pod, String name, String type) {
        return new Relation(kind, pod.getMetadata().getNamespace(), name, type);
    }

    private Map<String, String> safeAnnotations(HasMetadata resource) {
        return safeMap(resource.getMetadata().getAnnotations()).entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(KAFKA_ANNOTATION_PREFIX))
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, String> safeMap(Map<String, String> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private <T> T execute(String operation, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (KubernetesClientException exception) {
            throw new KubernetesApiException(operation, exception);
        }
    }
}
