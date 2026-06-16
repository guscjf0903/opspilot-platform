package com.opspilot.kubernetes.domain;

public record DeploymentStatusSnapshot(
        int desiredReplicas,
        int availableReplicas,
        int unavailableReplicas
) {
}
