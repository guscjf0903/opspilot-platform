package com.opspilot.action.application;

import com.opspilot.action.application.port.out.ActionApprovalStore;
import com.opspilot.action.application.port.out.ActionAuditLogStore;
import com.opspilot.action.application.port.out.KubernetesActionPort;
import com.opspilot.action.config.ActionProperties;
import com.opspilot.action.domain.ActionActor;
import com.opspilot.action.domain.ActionApprovalDecisionRequest;
import com.opspilot.action.domain.ActionApprovalRequest;
import com.opspilot.action.domain.ActionAuditLog;
import com.opspilot.action.domain.ActionCommand;
import com.opspilot.action.domain.ActionExecutionResult;
import com.opspilot.action.domain.ActionPreview;
import com.opspilot.action.domain.ActionRequest;
import com.opspilot.action.domain.ActionRiskLevel;
import com.opspilot.action.domain.ActionStatus;
import com.opspilot.action.domain.ActionType;
import com.opspilot.action.domain.UserRole;
import com.opspilot.kubernetes.application.UnknownClusterException;
import com.opspilot.kubernetes.config.OpspilotKubernetesProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActionService {

    private final KubernetesActionPort kubernetesActionPort;
    private final ActionAuditLogStore auditLogStore;
    private final ActionApprovalStore approvalStore;
    private final ActionPermissionService permissionService;
    private final ActionPolicyService policyService;
    private final ActionProperties actionProperties;
    private final OpspilotKubernetesProperties kubernetesProperties;

    @Transactional
    public ActionPreview dryRun(String clusterId, ActionActor actor, ActionRequest request) {
        ActionCommand command = command(clusterId, request);
        permissionService.assertCanRequest(actor, command);

        boolean productionNamespace = permissionService.isProductionNamespace(command.namespace());
        policyService.validate(command, productionNamespace);

        ActionPreview adapterPreview = kubernetesActionPort.preview(command);
        ActionPreview preview = withPolicy(adapterPreview, actor, command, productionNamespace);
        auditLogStore.save(auditLog(
                UUID.randomUUID(),
                actor,
                command,
                preview,
                ActionStatus.DRY_RUN,
                null,
                "dry-run을 생성했습니다.",
                null
        ));

        return preview;
    }

    @Transactional
    public ActionExecutionResult execute(String clusterId, ActionActor actor, ActionRequest request) {
        if (!actionProperties.isExecutionEnabled()) {
            throw new ActionForbiddenException("Kubernetes action execution is disabled in this environment.");
        }

        ActionCommand command = command(clusterId, request);
        permissionService.assertCanRequest(actor, command);

        boolean productionNamespace = permissionService.isProductionNamespace(command.namespace());
        policyService.validate(command, productionNamespace);

        ActionPreview preview = withPolicy(kubernetesActionPort.preview(command), actor, command, productionNamespace);
        if (preview.approvalRequired() && !approved(command)) {
            return requestApproval(actor, command, preview);
        }

        UUID auditLogId = UUID.randomUUID();
        try {
            ActionPreview executedPreview = withPolicy(
                    kubernetesActionPort.execute(command),
                    actor,
                    command,
                    productionNamespace
            );
            Instant executedAt = Instant.now();
            UUID approvalId = approvalId(command);
            ActionAuditLog auditLog = auditLog(
                    auditLogId,
                    actor,
                    command,
                    executedPreview,
                    ActionStatus.SUCCESS,
                    approvalId,
                    "조치가 성공적으로 실행되었습니다.",
                    executedAt
            );

            auditLogStore.save(auditLog);
            return new ActionExecutionResult(
                    auditLogId,
                    approvalId,
                    ActionStatus.SUCCESS,
                    executedPreview,
                    auditLog.message(),
                    executedAt
            );
        } catch (RuntimeException exception) {
            Instant executedAt = Instant.now();
            ActionAuditLog auditLog = auditLog(
                    auditLogId,
                    actor,
                    command,
                    preview,
                    ActionStatus.FAILED,
                    null,
                    exception.getMessage(),
                    executedAt
            );
            auditLogStore.save(auditLog);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ActionAuditLog> getAuditLogs(String clusterId, String namespace, String targetName) {
        validateCluster(clusterId);
        return auditLogStore.find(clusterId, namespace, targetName);
    }

    @Transactional(readOnly = true)
    public List<ActionApprovalRequest> getApprovals(String clusterId, ActionStatus status) {
        validateCluster(clusterId);
        return approvalStore.find(clusterId, status);
    }

    @Transactional
    public ActionApprovalRequest approve(
            String clusterId,
            UUID approvalId,
            ActionActor actor,
            ActionApprovalDecisionRequest request
    ) {
        return decide(clusterId, approvalId, actor, request, ActionStatus.APPROVED);
    }

    @Transactional
    public ActionApprovalRequest reject(
            String clusterId,
            UUID approvalId,
            ActionActor actor,
            ActionApprovalDecisionRequest request
    ) {
        return decide(clusterId, approvalId, actor, request, ActionStatus.REJECTED);
    }

    private ActionApprovalRequest decide(
            String clusterId,
            UUID approvalId,
            ActionActor actor,
            ActionApprovalDecisionRequest request,
            ActionStatus status
    ) {
        validateCluster(clusterId);
        if (actor.role() != UserRole.ADMIN && actor.role() != UserRole.OPERATOR) {
            throw new ActionForbiddenException("Only Admin or Operator can decide action approvals.");
        }

        ActionApprovalRequest approval = approvalStore.findById(approvalId)
                .filter(item -> item.clusterId().equals(clusterId))
                .orElseThrow(() -> new ActionApprovalNotFoundException(approvalId));
        if (approval.status() != ActionStatus.PENDING_APPROVAL) {
            throw new ActionValidationException("Approval request is already decided.");
        }

        return approvalStore.save(new ActionApprovalRequest(
                approval.id(),
                approval.auditLogId(),
                approval.clusterId(),
                approval.namespace(),
                approval.targetKind(),
                approval.targetName(),
                approval.actionType(),
                approval.requester(),
                approval.requesterRole(),
                status,
                approval.requestedAt(),
                actor.email(),
                Instant.now(),
                request == null ? null : request.reason()
        ));
    }

    private ActionExecutionResult requestApproval(ActionActor actor, ActionCommand command, ActionPreview preview) {
        UUID auditLogId = UUID.randomUUID();
        UUID approvalId = UUID.randomUUID();
        ActionAuditLog auditLog = auditLog(
                auditLogId,
                actor,
                command,
                preview,
                ActionStatus.PENDING_APPROVAL,
                approvalId,
                "승인이 필요한 조치입니다. 승인 후 같은 요청에 approvalId를 포함해 다시 실행하세요.",
                null
        );
        ActionApprovalRequest approvalRequest = new ActionApprovalRequest(
                approvalId,
                auditLogId,
                command.clusterId(),
                command.namespace(),
                command.targetKind(),
                command.targetName(),
                command.type(),
                actor.email(),
                actor.role(),
                ActionStatus.PENDING_APPROVAL,
                Instant.now(),
                null,
                null,
                null
        );

        auditLogStore.save(auditLog);
        approvalStore.save(approvalRequest);

        return new ActionExecutionResult(
                auditLogId,
                approvalId,
                ActionStatus.PENDING_APPROVAL,
                preview,
                auditLog.message(),
                null
        );
    }

    private boolean approved(ActionCommand command) {
        UUID approvalId = approvalId(command);
        if (approvalId == null) {
            return false;
        }

        return approvalStore.findById(approvalId)
                .filter(approval -> approval.status() == ActionStatus.APPROVED)
                .filter(approval -> approval.clusterId().equals(command.clusterId()))
                .filter(approval -> approval.namespace().equals(command.namespace()))
                .filter(approval -> approval.targetKind().equals(command.targetKind()))
                .filter(approval -> approval.targetName().equals(command.targetName()))
                .filter(approval -> approval.actionType() == command.type())
                .isPresent();
    }

    private UUID approvalId(ActionCommand command) {
        String approvalId = command.parameters().get("approvalId");
        if (approvalId == null || approvalId.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(approvalId);
        } catch (IllegalArgumentException exception) {
            throw new ActionValidationException("approvalId must be a UUID.");
        }
    }

    private ActionPreview withPolicy(
            ActionPreview preview,
            ActionActor actor,
            ActionCommand command,
            boolean productionNamespace
    ) {
        ActionRiskLevel risk = policyService.risk(command, productionNamespace);
        boolean approvalRequired = permissionService.approvalRequired(actor, command);
        List<String> warnings = new ArrayList<>(preview.warnings());
        warnings.addAll(policyService.warnings(command, productionNamespace));

        return new ActionPreview(
                preview.clusterId(),
                preview.type(),
                preview.namespace(),
                preview.targetKind(),
                preview.targetName(),
                risk,
                approvalRequired,
                preview.executable(),
                preview.beforeState(),
                preview.afterState(),
                preview.diff(),
                warnings.stream().distinct().toList(),
                preview.message(),
                preview.createdAt()
        );
    }

    private ActionAuditLog auditLog(
            UUID auditLogId,
            ActionActor actor,
            ActionCommand command,
            ActionPreview preview,
            ActionStatus status,
            UUID approvalId,
            String message,
            Instant executedAt
    ) {
        return new ActionAuditLog(
                auditLogId,
                command.clusterId(),
                command.namespace(),
                command.targetKind(),
                command.targetName(),
                command.type(),
                actor.email(),
                actor.role(),
                status,
                preview.risk(),
                preview.approvalRequired(),
                approvalId,
                command.parameters(),
                preview.beforeState(),
                preview.afterState(),
                preview.diff(),
                message,
                Instant.now(),
                executedAt
        );
    }

    private ActionCommand command(String clusterId, ActionRequest request) {
        validateCluster(clusterId);
        ActionType type = parseType(request.type());
        String targetKind = normalizeKind(request.targetKind());
        validateTargetKind(type, targetKind);

        return new ActionCommand(
                clusterId,
                type,
                request.namespace().trim(),
                targetKind,
                request.targetName().trim(),
                request.safeParameters(),
                request.reason()
        );
    }

    private ActionType parseType(String value) {
        try {
            return ActionType.fromValue(value);
        } catch (IllegalArgumentException exception) {
            throw new UnsupportedActionException(value);
        }
    }

    private String normalizeKind(String kind) {
        if (kind == null || kind.isBlank()) {
            throw new ActionValidationException("targetKind is required.");
        }

        String normalized = kind.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "deployment", "deployments" -> "Deployment";
            case "pod", "pods" -> "Pod";
            default -> throw new ActionValidationException("Unsupported targetKind: " + kind);
        };
    }

    private void validateTargetKind(ActionType type, String targetKind) {
        if (type == ActionType.DELETE_POD && !"Pod".equals(targetKind)) {
            throw new ActionValidationException("DELETE_POD targetKind must be Pod.");
        }

        if (type != ActionType.DELETE_POD && !"Deployment".equals(targetKind)) {
            throw new ActionValidationException(type + " targetKind must be Deployment.");
        }
    }

    private void validateCluster(String clusterId) {
        if (!kubernetesProperties.getClusterId().equals(clusterId)) {
            throw new UnknownClusterException(clusterId);
        }
    }
}
