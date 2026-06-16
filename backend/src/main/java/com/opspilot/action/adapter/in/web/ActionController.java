package com.opspilot.action.adapter.in.web;

import com.opspilot.action.application.ActionService;
import com.opspilot.action.application.ActionValidationException;
import com.opspilot.action.domain.ActionActor;
import com.opspilot.action.domain.ActionApprovalDecisionRequest;
import com.opspilot.action.domain.ActionApprovalRequest;
import com.opspilot.action.domain.ActionAuditLog;
import com.opspilot.action.domain.ActionExecutionResult;
import com.opspilot.action.domain.ActionPreview;
import com.opspilot.action.domain.ActionRequest;
import com.opspilot.action.domain.ActionStatus;
import com.opspilot.action.domain.UserRole;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clusters/{clusterId}/actions")
@RequiredArgsConstructor
public class ActionController {

    private final ActionService actionService;

    @PostMapping("/dry-run")
    public ActionPreview dryRun(
            @PathVariable String clusterId,
            @RequestBody @Valid ActionRequest request,
            @RequestHeader(name = "X-OpsPilot-Actor", required = false) String actorEmail,
            @RequestHeader(name = "X-OpsPilot-Role", required = false) String role
    ) {
        return actionService.dryRun(clusterId, actor(actorEmail, role), request);
    }

    @PostMapping("/execute")
    public ActionExecutionResult execute(
            @PathVariable String clusterId,
            @RequestBody @Valid ActionRequest request,
            @RequestHeader(name = "X-OpsPilot-Actor", required = false) String actorEmail,
            @RequestHeader(name = "X-OpsPilot-Role", required = false) String role
    ) {
        return actionService.execute(clusterId, actor(actorEmail, role), request);
    }

    @GetMapping("/audit-logs")
    public List<ActionAuditLog> getAuditLogs(
            @PathVariable String clusterId,
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) String targetName
    ) {
        return actionService.getAuditLogs(clusterId, namespace, targetName);
    }

    @GetMapping("/approvals")
    public List<ActionApprovalRequest> getApprovals(
            @PathVariable String clusterId,
            @RequestParam(required = false) String status
    ) {
        return actionService.getApprovals(clusterId, status(status));
    }

    @PostMapping("/approvals/{approvalId}/approve")
    public ActionApprovalRequest approve(
            @PathVariable String clusterId,
            @PathVariable UUID approvalId,
            @RequestBody(required = false) ActionApprovalDecisionRequest request,
            @RequestHeader(name = "X-OpsPilot-Actor", required = false) String actorEmail,
            @RequestHeader(name = "X-OpsPilot-Role", required = false) String role
    ) {
        return actionService.approve(clusterId, approvalId, actor(actorEmail, role), request);
    }

    @PostMapping("/approvals/{approvalId}/reject")
    public ActionApprovalRequest reject(
            @PathVariable String clusterId,
            @PathVariable UUID approvalId,
            @RequestBody(required = false) ActionApprovalDecisionRequest request,
            @RequestHeader(name = "X-OpsPilot-Actor", required = false) String actorEmail,
            @RequestHeader(name = "X-OpsPilot-Role", required = false) String role
    ) {
        return actionService.reject(clusterId, approvalId, actor(actorEmail, role), request);
    }

    private ActionActor actor(String actorEmail, String role) {
        try {
            return new ActionActor(
                    actorEmail == null || actorEmail.isBlank() ? ActionActor.localDefault().email() : actorEmail,
                    UserRole.fromValue(role)
            );
        } catch (IllegalArgumentException exception) {
            throw new ActionValidationException("Unsupported role: " + role);
        }
    }

    private ActionStatus status(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return ActionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ActionValidationException("Unsupported approval status: " + status);
        }
    }
}
