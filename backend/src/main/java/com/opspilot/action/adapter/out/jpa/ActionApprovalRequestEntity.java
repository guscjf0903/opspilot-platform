package com.opspilot.action.adapter.out.jpa;

import com.opspilot.action.domain.ActionApprovalRequest;
import com.opspilot.action.domain.ActionStatus;
import com.opspilot.action.domain.ActionType;
import com.opspilot.action.domain.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "action_approval_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ActionApprovalRequestEntity {

    @Id
    private UUID id;

    @Column(name = "audit_log_id", nullable = false)
    private UUID auditLogId;

    @Column(name = "cluster_id", nullable = false)
    private String clusterId;

    @Column(nullable = false)
    private String namespace;

    @Column(name = "target_kind", nullable = false)
    private String targetKind;

    @Column(name = "target_name", nullable = false)
    private String targetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Column(nullable = false)
    private String requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "requester_role", nullable = false)
    private UserRole requesterRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_reason")
    private String decisionReason;

    private ActionApprovalRequestEntity(ActionApprovalRequest request) {
        update(request);
    }

    static ActionApprovalRequestEntity fromDomain(ActionApprovalRequest request) {
        return new ActionApprovalRequestEntity(request);
    }

    void update(ActionApprovalRequest request) {
        this.id = request.id();
        this.auditLogId = request.auditLogId();
        this.clusterId = request.clusterId();
        this.namespace = request.namespace();
        this.targetKind = request.targetKind();
        this.targetName = request.targetName();
        this.actionType = request.actionType();
        this.requester = request.requester();
        this.requesterRole = request.requesterRole();
        this.status = request.status();
        this.requestedAt = request.requestedAt();
        this.decidedBy = request.decidedBy();
        this.decidedAt = request.decidedAt();
        this.decisionReason = request.decisionReason();
    }

    ActionApprovalRequest toDomain() {
        return new ActionApprovalRequest(
                id,
                auditLogId,
                clusterId,
                namespace,
                targetKind,
                targetName,
                actionType,
                requester,
                requesterRole,
                status,
                requestedAt,
                decidedBy,
                decidedAt,
                decisionReason
        );
    }
}
