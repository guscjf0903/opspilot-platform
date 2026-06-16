package com.opspilot.kubernetes.domain;

import java.util.Map;
import java.util.Set;

public class KubernetesResourceStatusEvaluator {

    private static final Set<String> CRITICAL_WAITING_REASONS = Set.of(
            "CrashLoopBackOff",
            "ImagePullBackOff",
            "ErrImagePull"
    );

    public ResourceHealth evaluateNamespace(NamespaceStatusSnapshot snapshot) {
        if ("Active".equals(snapshot.phase())) {
            return ResourceHealth.healthy();
        }

        if ("Terminating".equals(snapshot.phase())) {
            return ResourceHealth.warning("Terminating", "Namespace is terminating");
        }

        return ResourceHealth.unknown("UnknownPhase", "Namespace phase is not available");
    }

    public ResourceHealth evaluateDeployment(DeploymentStatusSnapshot snapshot) {
        if (snapshot.availableReplicas() < snapshot.desiredReplicas()
                || snapshot.unavailableReplicas() > 0) {
            return ResourceHealth.warning(
                    "UnavailableReplicas",
                    "Available replicas %d/%d".formatted(
                            snapshot.availableReplicas(),
                            snapshot.desiredReplicas()
                    )
            );
        }

        return ResourceHealth.healthy();
    }

    public ResourceHealth evaluatePod(PodStatusSnapshot snapshot) {
        String criticalWaitingReason = snapshot.waitingReasons().stream()
                .filter(CRITICAL_WAITING_REASONS::contains)
                .findFirst()
                .orElse(null);

        if (criticalWaitingReason != null) {
            return ResourceHealth.critical(
                    criticalWaitingReason,
                    "Container is waiting: " + criticalWaitingReason
            );
        }

        if (snapshot.currentTerminatedReasons().contains("OOMKilled")
                || snapshot.previousTerminatedReasons().contains("OOMKilled")) {
            return ResourceHealth.critical("OOMKilled", "Container was terminated because it exceeded its memory limit");
        }

        String criticalTerminatedReason = snapshot.currentTerminatedReasons().stream()
                .filter(reason -> !"Completed".equals(reason))
                .findFirst()
                .orElse(null);

        if (criticalTerminatedReason != null) {
            return ResourceHealth.critical(
                    criticalTerminatedReason,
                    "Container is terminated: " + criticalTerminatedReason
            );
        }

        if ("Pending".equals(snapshot.phase())) {
            return ResourceHealth.warning("Pending", "Pod is waiting to be scheduled or initialized");
        }

        if ("Failed".equals(snapshot.phase())) {
            return ResourceHealth.critical("Failed", "Pod execution failed");
        }

        if (snapshot.restartCount() > 0) {
            return ResourceHealth.warning(
                    "Restarted",
                    "Pod containers restarted %d times".formatted(snapshot.restartCount())
            );
        }

        if ("Running".equals(snapshot.phase()) || "Succeeded".equals(snapshot.phase())) {
            return ResourceHealth.healthy();
        }

        return ResourceHealth.unknown("UnknownPhase", "Pod phase is not available");
    }

    public ResourceHealth evaluateNode(NodeStatusSnapshot snapshot) {
        Map<String, String> conditions = snapshot.conditions();
        String ready = conditions.get("Ready");

        if ("False".equals(ready)) {
            return ResourceHealth.critical("NotReady", "Node is not ready");
        }

        if ("Unknown".equals(ready) || ready == null) {
            return ResourceHealth.unknown("ReadyUnknown", "Node readiness is unknown");
        }

        if ("True".equals(conditions.get("MemoryPressure"))) {
            return ResourceHealth.warning("MemoryPressure", "Node is under memory pressure");
        }

        if ("True".equals(conditions.get("DiskPressure"))) {
            return ResourceHealth.warning("DiskPressure", "Node is under disk pressure");
        }

        if ("True".equals(conditions.get("PIDPressure"))) {
            return ResourceHealth.warning("PIDPressure", "Node is under PID pressure");
        }

        return ResourceHealth.healthy();
    }

    public ResourceHealth evaluateEvent(EventStatusSnapshot snapshot) {
        if ("Warning".equalsIgnoreCase(snapshot.type())) {
            return ResourceHealth.warning(
                    valueOrDefault(snapshot.reason(), "Warning"),
                    valueOrDefault(snapshot.message(), "Kubernetes warning event")
            );
        }

        return ResourceHealth.healthy();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
