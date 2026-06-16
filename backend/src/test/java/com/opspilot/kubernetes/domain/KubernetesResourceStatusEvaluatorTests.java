package com.opspilot.kubernetes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KubernetesResourceStatusEvaluatorTests {

    private final KubernetesResourceStatusEvaluator evaluator = new KubernetesResourceStatusEvaluator();

    @Test
    void marksCrashLoopBackOffPodAsCritical() {
        ResourceHealth health = evaluator.evaluatePod(new PodStatusSnapshot(
                "Running",
                List.of("CrashLoopBackOff"),
                List.of(),
                List.of(),
                3
        ));

        assertThat(health.status()).isEqualTo(ResourceStatus.CRITICAL);
        assertThat(health.reason()).isEqualTo("CrashLoopBackOff");
    }

    @Test
    void marksOomKilledPodAsCritical() {
        ResourceHealth health = evaluator.evaluatePod(new PodStatusSnapshot(
                "Running",
                List.of(),
                List.of(),
                List.of("OOMKilled"),
                1
        ));

        assertThat(health.status()).isEqualTo(ResourceStatus.CRITICAL);
        assertThat(health.reason()).isEqualTo("OOMKilled");
    }

    @Test
    void distinguishesPendingPodFromCriticalFailures() {
        ResourceHealth health = evaluator.evaluatePod(new PodStatusSnapshot(
                "Pending",
                List.of(),
                List.of(),
                List.of(),
                0
        ));

        assertThat(health.status()).isEqualTo(ResourceStatus.WARNING);
        assertThat(health.reason()).isEqualTo("Pending");
    }

    @Test
    void marksCurrentlyTerminatedErrorPodAsCritical() {
        ResourceHealth health = evaluator.evaluatePod(new PodStatusSnapshot(
                "Running",
                List.of(),
                List.of("Error"),
                List.of(),
                3
        ));

        assertThat(health.status()).isEqualTo(ResourceStatus.CRITICAL);
        assertThat(health.reason()).isEqualTo("Error");
    }

    @Test
    void keepsRecoveredPodWithPreviousErrorAtWarning() {
        ResourceHealth health = evaluator.evaluatePod(new PodStatusSnapshot(
                "Running",
                List.of(),
                List.of(),
                List.of("Error"),
                1
        ));

        assertThat(health.status()).isEqualTo(ResourceStatus.WARNING);
        assertThat(health.reason()).isEqualTo("Restarted");
    }

    @Test
    void marksDeploymentWithUnavailableReplicasAsWarning() {
        ResourceHealth health = evaluator.evaluateDeployment(new DeploymentStatusSnapshot(3, 2, 1));

        assertThat(health.status()).isEqualTo(ResourceStatus.WARNING);
        assertThat(health.reason()).isEqualTo("UnavailableReplicas");
    }

    @Test
    void marksNodeWithDiskPressureAsWarning() {
        ResourceHealth health = evaluator.evaluateNode(new NodeStatusSnapshot(Map.of(
                "Ready", "True",
                "DiskPressure", "True"
        )));

        assertThat(health.status()).isEqualTo(ResourceStatus.WARNING);
        assertThat(health.reason()).isEqualTo("DiskPressure");
    }
}
