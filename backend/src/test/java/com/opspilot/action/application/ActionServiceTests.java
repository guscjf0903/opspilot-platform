package com.opspilot.action.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.opspilot.action.application.port.out.ActionApprovalStore;
import com.opspilot.action.application.port.out.ActionAuditLogStore;
import com.opspilot.action.application.port.out.KubernetesActionPort;
import com.opspilot.action.domain.ActionActor;
import com.opspilot.action.domain.ActionApprovalRequest;
import com.opspilot.action.domain.ActionAuditLog;
import com.opspilot.action.domain.ActionCommand;
import com.opspilot.action.domain.ActionDiff;
import com.opspilot.action.domain.ActionExecutionResult;
import com.opspilot.action.domain.ActionPreview;
import com.opspilot.action.domain.ActionRequest;
import com.opspilot.action.domain.ActionRiskLevel;
import com.opspilot.action.domain.ActionStatus;
import com.opspilot.action.domain.ActionType;
import com.opspilot.action.domain.UserRole;
import com.opspilot.kubernetes.config.OpspilotKubernetesProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActionServiceTests {

    private InMemoryAuditLogStore auditLogStore;
    private InMemoryApprovalStore approvalStore;
    private ActionService actionService;

    @BeforeEach
    void setUp() {
        OpspilotKubernetesProperties properties = new OpspilotKubernetesProperties();
        properties.setClusterId("local");
        auditLogStore = new InMemoryAuditLogStore();
        approvalStore = new InMemoryApprovalStore();
        actionService = new ActionService(
                new FakeKubernetesActionPort(),
                auditLogStore,
                approvalStore,
                new ActionPermissionService(),
                new ActionPolicyService(),
                properties
        );
    }

    @Test
    void dryRunReturnsDiffAndStoresAuditLog() {
        ActionPreview preview = actionService.dryRun(
                "local",
                new ActionActor("operator@example.com", UserRole.OPERATOR),
                new ActionRequest(
                        "SCALE_DEPLOYMENT",
                        "sample-app",
                        "Deployment",
                        "payment-api",
                        Map.of("replicas", "3"),
                        "scale test"
                )
        );

        assertThat(preview.diff()).extracting(ActionDiff::field).contains("spec.replicas");
        assertThat(preview.approvalRequired()).isFalse();
        assertThat(auditLogStore.logs).hasSize(1);
        assertThat(auditLogStore.logs.getFirst().status()).isEqualTo(ActionStatus.DRY_RUN);
    }

    @Test
    void productionActionCreatesApprovalRequestInsteadOfExecuting() {
        ActionExecutionResult result = actionService.execute(
                "local",
                new ActionActor("operator@example.com", UserRole.OPERATOR),
                new ActionRequest(
                        "SCALE_DEPLOYMENT",
                        "production",
                        "Deployment",
                        "payment-api",
                        Map.of("replicas", "3"),
                        "production scale"
                )
        );

        assertThat(result.status()).isEqualTo(ActionStatus.PENDING_APPROVAL);
        assertThat(result.approvalId()).isNotNull();
        assertThat(approvalStore.approvals).hasSize(1);
        assertThat(auditLogStore.logs.getFirst().status()).isEqualTo(ActionStatus.PENDING_APPROVAL);
    }

    @Test
    void viewerCannotRequestActions() {
        assertThatThrownBy(() -> actionService.dryRun(
                "local",
                new ActionActor("viewer@example.com", UserRole.VIEWER),
                new ActionRequest(
                        "RESTART_DEPLOYMENT",
                        "sample-app",
                        "Deployment",
                        "payment-api",
                        Map.of(),
                        null
                )
        )).isInstanceOf(ActionForbiddenException.class);
    }

    private static class FakeKubernetesActionPort implements KubernetesActionPort {

        @Override
        public ActionPreview preview(ActionCommand command) {
            return preview(command, false);
        }

        @Override
        public ActionPreview execute(ActionCommand command) {
            return preview(command, true);
        }

        private ActionPreview preview(ActionCommand command, boolean executed) {
            return new ActionPreview(
                    command.clusterId(),
                    command.type(),
                    command.namespace(),
                    command.targetKind(),
                    command.targetName(),
                    ActionRiskLevel.LOW,
                    false,
                    true,
                    Map.of("desiredReplicas", 2),
                    Map.of("desiredReplicas", command.parameters().getOrDefault("replicas", "2"), "executed", executed),
                    command.type() == ActionType.SCALE_DEPLOYMENT
                            ? List.of(new ActionDiff("spec.replicas", "2", command.parameters().get("replicas")))
                            : List.of(new ActionDiff("metadata.annotation", "-", "now")),
                    List.of(),
                    "preview",
                    Instant.now()
            );
        }
    }

    private static class InMemoryAuditLogStore implements ActionAuditLogStore {

        private final List<ActionAuditLog> logs = new ArrayList<>();

        @Override
        public ActionAuditLog save(ActionAuditLog auditLog) {
            logs.add(auditLog);
            return auditLog;
        }

        @Override
        public List<ActionAuditLog> find(String clusterId, String namespace, String targetName) {
            return logs;
        }
    }

    private static class InMemoryApprovalStore implements ActionApprovalStore {

        private final List<ActionApprovalRequest> approvals = new ArrayList<>();

        @Override
        public ActionApprovalRequest save(ActionApprovalRequest approvalRequest) {
            approvals.removeIf(item -> item.id().equals(approvalRequest.id()));
            approvals.add(approvalRequest);
            return approvalRequest;
        }

        @Override
        public Optional<ActionApprovalRequest> findById(UUID id) {
            return approvals.stream().filter(item -> item.id().equals(id)).findFirst();
        }

        @Override
        public List<ActionApprovalRequest> find(String clusterId, ActionStatus status) {
            return approvals;
        }
    }
}
