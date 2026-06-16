package com.opspilot.action.adapter.out.fabric8;

import com.opspilot.action.application.ActionTargetNotFoundException;
import com.opspilot.action.application.ActionValidationException;
import com.opspilot.action.application.KubernetesActionException;
import com.opspilot.action.application.port.out.KubernetesActionPort;
import com.opspilot.action.domain.ActionCommand;
import com.opspilot.action.domain.ActionDiff;
import com.opspilot.action.domain.ActionPreview;
import com.opspilot.action.domain.ActionRiskLevel;
import com.opspilot.action.domain.ActionType;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Fabric8KubernetesActionAdapter implements KubernetesActionPort {

    private static final String RESTARTED_AT_ANNOTATION = "opspilot.io/restartedAt";
    private static final String ROLLBACK_AT_ANNOTATION = "opspilot.io/rollbackAt";
    private static final String REVISION_ANNOTATION = "deployment.kubernetes.io/revision";

    private final KubernetesClient kubernetesClient;

    @Override
    public ActionPreview preview(ActionCommand command) {
        return switch (command.type()) {
            case RESTART_DEPLOYMENT -> restartDeploymentPreview(command, false);
            case SCALE_DEPLOYMENT -> scaleDeploymentPreview(command, false);
            case ROLLOUT_UNDO -> rolloutUndoPreview(command, false);
            case DELETE_POD -> deletePodPreview(command, false);
        };
    }

    @Override
    public ActionPreview execute(ActionCommand command) {
        return switch (command.type()) {
            case RESTART_DEPLOYMENT -> restartDeploymentPreview(command, true);
            case SCALE_DEPLOYMENT -> scaleDeploymentPreview(command, true);
            case ROLLOUT_UNDO -> rolloutUndoPreview(command, true);
            case DELETE_POD -> deletePodPreview(command, true);
        };
    }

    private ActionPreview restartDeploymentPreview(ActionCommand command, boolean execute) {
        try {
            Deployment deployment = deployment(command.namespace(), command.targetName());
            String beforeRestartedAt = templateAnnotations(deployment).get(RESTARTED_AT_ANNOTATION);
            String restartedAt = Instant.now().toString();
            Map<String, Object> beforeState = deploymentState(deployment);
            Map<String, Object> afterState = new LinkedHashMap<>(beforeState);
            afterState.put("restartedAt", restartedAt);

            if (execute) {
                Deployment updated = kubernetesClient.apps()
                        .deployments()
                        .inNamespace(command.namespace())
                        .withName(command.targetName())
                        .edit(current -> {
                            putTemplateAnnotation(current, RESTARTED_AT_ANNOTATION, restartedAt);
                            return current;
                        });
                afterState = deploymentState(updated);
            }

            return preview(
                    command,
                    beforeState,
                    afterState,
                    List.of(new ActionDiff("spec.template.metadata.annotations." + RESTARTED_AT_ANNOTATION,
                            nullToDash(beforeRestartedAt),
                            restartedAt)),
                    "Deployment restart를 위해 Pod template annotation을 갱신합니다."
            );
        } catch (KubernetesClientException exception) {
            throw new KubernetesActionException("Failed to restart deployment.", exception);
        }
    }

    private ActionPreview scaleDeploymentPreview(ActionCommand command, boolean execute) {
        try {
            Deployment deployment = deployment(command.namespace(), command.targetName());
            int beforeReplicas = replicas(deployment);
            int afterReplicas = replicasParameter(command);
            Map<String, Object> beforeState = deploymentState(deployment);
            Map<String, Object> afterState = new LinkedHashMap<>(beforeState);
            afterState.put("desiredReplicas", afterReplicas);

            if (execute) {
                Deployment updated = kubernetesClient.apps()
                        .deployments()
                        .inNamespace(command.namespace())
                        .withName(command.targetName())
                        .scale(afterReplicas);
                afterState = deploymentState(updated);
            }

            return preview(
                    command,
                    beforeState,
                    afterState,
                    List.of(new ActionDiff("spec.replicas", String.valueOf(beforeReplicas), String.valueOf(afterReplicas))),
                    "Deployment replica 수를 변경합니다."
            );
        } catch (KubernetesClientException exception) {
            throw new KubernetesActionException("Failed to scale deployment.", exception);
        }
    }

    private ActionPreview rolloutUndoPreview(ActionCommand command, boolean execute) {
        try {
            Deployment deployment = deployment(command.namespace(), command.targetName());
            ReplicaSet previousReplicaSet = previousReplicaSet(command.namespace(), deployment);
            String currentRevision = revision(deployment);
            String targetRevision = revision(previousReplicaSet);
            Map<String, Object> beforeState = deploymentState(deployment);
            beforeState.put("revision", currentRevision);
            beforeState.put("images", images(deployment.getSpec().getTemplate()));

            Map<String, Object> afterState = new LinkedHashMap<>(beforeState);
            afterState.put("revision", targetRevision);
            afterState.put("images", images(previousReplicaSet.getSpec().getTemplate()));

            if (execute) {
                String rollbackAt = Instant.now().toString();
                Deployment updated = kubernetesClient.apps()
                        .deployments()
                        .inNamespace(command.namespace())
                        .withName(command.targetName())
                        .edit(current -> {
                            current.getSpec().setTemplate(previousReplicaSet.getSpec().getTemplate());
                            putTemplateAnnotation(current, ROLLBACK_AT_ANNOTATION, rollbackAt);
                            return current;
                        });
                afterState = deploymentState(updated);
                afterState.put("targetRevision", targetRevision);
                afterState.put("rollbackAt", rollbackAt);
            }

            return preview(
                    command,
                    beforeState,
                    afterState,
                    List.of(
                            new ActionDiff("deployment.revision", currentRevision, targetRevision),
                            new ActionDiff("spec.template.images",
                                    String.join(", ", images(deployment.getSpec().getTemplate())),
                                    String.join(", ", images(previousReplicaSet.getSpec().getTemplate())))
                    ),
                    "Deployment를 이전 ReplicaSet template으로 되돌립니다."
            );
        } catch (KubernetesClientException exception) {
            throw new KubernetesActionException("Failed to rollback deployment.", exception);
        }
    }

    private ActionPreview deletePodPreview(ActionCommand command, boolean execute) {
        try {
            Pod pod = pod(command.namespace(), command.targetName());
            Map<String, Object> beforeState = podState(pod);
            Map<String, Object> afterState = new LinkedHashMap<>(beforeState);
            afterState.put("deleted", true);
            afterState.put("phase", "Deleting");
            List<String> warnings = new ArrayList<>();
            if (ownerReferences(pod).isEmpty()) {
                warnings.add("ownerReferences가 없는 Pod는 삭제 후 자동 재생성되지 않을 수 있습니다.");
            }

            if (execute) {
                kubernetesClient.pods()
                        .inNamespace(command.namespace())
                        .withName(command.targetName())
                        .delete();
            }

            return new ActionPreview(
                    command.clusterId(),
                    command.type(),
                    command.namespace(),
                    command.targetKind(),
                    command.targetName(),
                    ActionRiskLevel.LOW,
                    false,
                    true,
                    beforeState,
                    afterState,
                    List.of(new ActionDiff("pod.deleted", "false", "true")),
                    warnings,
                    "Pod를 삭제합니다. controller가 관리하는 Pod라면 새 Pod가 생성됩니다.",
                    Instant.now()
            );
        } catch (KubernetesClientException exception) {
            throw new KubernetesActionException("Failed to delete pod.", exception);
        }
    }

    private ActionPreview preview(
            ActionCommand command,
            Map<String, Object> beforeState,
            Map<String, Object> afterState,
            List<ActionDiff> diff,
            String message
    ) {
        return new ActionPreview(
                command.clusterId(),
                command.type(),
                command.namespace(),
                command.targetKind(),
                command.targetName(),
                ActionRiskLevel.LOW,
                false,
                true,
                beforeState,
                afterState,
                diff,
                List.of(),
                message,
                Instant.now()
        );
    }

    private Deployment deployment(String namespace, String name) {
        Deployment deployment = kubernetesClient.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (deployment == null) {
            throw new ActionTargetNotFoundException("Deployment", name);
        }

        return deployment;
    }

    private Pod pod(String namespace, String name) {
        Pod pod = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(name)
                .get();
        if (pod == null) {
            throw new ActionTargetNotFoundException("Pod", name);
        }

        return pod;
    }

    private ReplicaSet previousReplicaSet(String namespace, Deployment deployment) {
        String deploymentUid = deployment.getMetadata().getUid();
        List<ReplicaSet> replicaSets = kubernetesClient.apps()
                .replicaSets()
                .inNamespace(namespace)
                .list()
                .getItems()
                .stream()
                .filter(replicaSet -> ownedByDeployment(replicaSet, deploymentUid, deployment.getMetadata().getName()))
                .sorted(Comparator.comparingLong(this::revisionNumber).reversed())
                .toList();

        if (replicaSets.size() < 2) {
            throw new ActionValidationException("Rollback target ReplicaSet was not found.");
        }

        return replicaSets.get(1);
    }

    private boolean ownedByDeployment(ReplicaSet replicaSet, String deploymentUid, String deploymentName) {
        return ownerReferences(replicaSet).stream()
                .anyMatch(owner -> "Deployment".equals(owner.getKind())
                        && deploymentName.equals(owner.getName())
                        && (deploymentUid == null || deploymentUid.equals(owner.getUid())));
    }

    private List<OwnerReference> ownerReferences(ReplicaSet replicaSet) {
        return replicaSet.getMetadata() == null || replicaSet.getMetadata().getOwnerReferences() == null
                ? List.of()
                : replicaSet.getMetadata().getOwnerReferences();
    }

    private List<OwnerReference> ownerReferences(Pod pod) {
        return pod.getMetadata() == null || pod.getMetadata().getOwnerReferences() == null
                ? List.of()
                : pod.getMetadata().getOwnerReferences();
    }

    private Map<String, Object> deploymentState(Deployment deployment) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("kind", "Deployment");
        state.put("namespace", deployment.getMetadata().getNamespace());
        state.put("name", deployment.getMetadata().getName());
        state.put("desiredReplicas", replicas(deployment));
        state.put("availableReplicas", deployment.getStatus() == null
                ? 0
                : valueOrZero(deployment.getStatus().getAvailableReplicas()));
        state.put("readyReplicas", deployment.getStatus() == null
                ? 0
                : valueOrZero(deployment.getStatus().getReadyReplicas()));
        state.put("updatedReplicas", deployment.getStatus() == null
                ? 0
                : valueOrZero(deployment.getStatus().getUpdatedReplicas()));
        state.put("restartedAt", templateAnnotations(deployment).get(RESTARTED_AT_ANNOTATION));
        return state;
    }

    private Map<String, Object> podState(Pod pod) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("kind", "Pod");
        state.put("namespace", pod.getMetadata().getNamespace());
        state.put("name", pod.getMetadata().getName());
        state.put("phase", pod.getStatus() == null ? null : pod.getStatus().getPhase());
        state.put("nodeName", pod.getSpec() == null ? null : pod.getSpec().getNodeName());
        state.put("owners", ownerReferences(pod).stream()
                .map(owner -> owner.getKind() + "/" + owner.getName())
                .toList());
        return state;
    }

    private int replicas(Deployment deployment) {
        if (deployment.getSpec() != null && deployment.getSpec().getReplicas() != null) {
            return deployment.getSpec().getReplicas();
        }

        return deployment.getStatus() == null ? 0 : valueOrZero(deployment.getStatus().getReplicas());
    }

    private int replicasParameter(ActionCommand command) {
        String raw = command.parameters().get("replicas");
        if (raw == null || raw.isBlank()) {
            throw new ActionValidationException("replicas parameter is required for SCALE_DEPLOYMENT.");
        }

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            throw new ActionValidationException("replicas must be an integer.");
        }
    }

    private Map<String, String> templateAnnotations(Deployment deployment) {
        if (deployment.getSpec() == null
                || deployment.getSpec().getTemplate() == null
                || deployment.getSpec().getTemplate().getMetadata() == null
                || deployment.getSpec().getTemplate().getMetadata().getAnnotations() == null) {
            return Map.of();
        }

        return deployment.getSpec().getTemplate().getMetadata().getAnnotations();
    }

    private void putTemplateAnnotation(Deployment deployment, String key, String value) {
        if (deployment.getSpec() == null || deployment.getSpec().getTemplate() == null) {
            throw new ActionValidationException("Deployment pod template is missing.");
        }

        ObjectMeta metadata = deployment.getSpec().getTemplate().getMetadata();
        if (metadata == null) {
            metadata = new ObjectMeta();
            deployment.getSpec().getTemplate().setMetadata(metadata);
        }

        Map<String, String> annotations = metadata.getAnnotations() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(metadata.getAnnotations());
        annotations.put(key, value);
        metadata.setAnnotations(annotations);
    }

    private String revision(Deployment deployment) {
        String revision = deployment.getMetadata() == null || deployment.getMetadata().getAnnotations() == null
                ? null
                : deployment.getMetadata().getAnnotations().get(REVISION_ANNOTATION);
        return revision == null ? "-" : revision;
    }

    private String revision(ReplicaSet replicaSet) {
        String revision = replicaSet.getMetadata() == null || replicaSet.getMetadata().getAnnotations() == null
                ? null
                : replicaSet.getMetadata().getAnnotations().get(REVISION_ANNOTATION);
        return revision == null ? "-" : revision;
    }

    private long revisionNumber(ReplicaSet replicaSet) {
        try {
            return Long.parseLong(revision(replicaSet));
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private List<String> images(PodTemplateSpec template) {
        if (template == null || template.getSpec() == null || template.getSpec().getContainers() == null) {
            return List.of();
        }

        return template.getSpec()
                .getContainers()
                .stream()
                .map(container -> container.getImage())
                .filter(Objects::nonNull)
                .toList();
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String nullToDash(String value) {
        return value == null ? "-" : value;
    }
}
