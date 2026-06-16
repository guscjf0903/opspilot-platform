package com.opspilot.action.adapter.out.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.action.domain.ActionAuditLog;
import com.opspilot.action.domain.ActionDiff;
import com.opspilot.action.domain.ActionRiskLevel;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "action_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ActionAuditLogEntity {

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final TypeReference<List<ActionDiff>> DIFF_LIST = new TypeReference<>() {
    };

    @Id
    private UUID id;

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
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false)
    private UserRole actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionRiskLevel risk;

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;

    @Column(name = "approval_id")
    private UUID approvalId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode parameters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state", nullable = false, columnDefinition = "jsonb")
    private JsonNode beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", nullable = false, columnDefinition = "jsonb")
    private JsonNode afterState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode diff;

    @Column(nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    private ActionAuditLogEntity(ActionAuditLog auditLog, ObjectMapper objectMapper) {
        this.id = auditLog.id();
        this.clusterId = auditLog.clusterId();
        this.namespace = auditLog.namespace();
        this.targetKind = auditLog.targetKind();
        this.targetName = auditLog.targetName();
        this.actionType = auditLog.actionType();
        this.actor = auditLog.actor();
        this.actorRole = auditLog.actorRole();
        this.status = auditLog.status();
        this.risk = auditLog.risk();
        this.approvalRequired = auditLog.approvalRequired();
        this.approvalId = auditLog.approvalId();
        this.parameters = objectMapper.valueToTree(auditLog.parameters());
        this.beforeState = objectMapper.valueToTree(auditLog.beforeState());
        this.afterState = objectMapper.valueToTree(auditLog.afterState());
        this.diff = objectMapper.valueToTree(auditLog.diff());
        this.message = auditLog.message();
        this.createdAt = auditLog.createdAt();
        this.executedAt = auditLog.executedAt();
    }

    static ActionAuditLogEntity fromDomain(ActionAuditLog auditLog, ObjectMapper objectMapper) {
        return new ActionAuditLogEntity(auditLog, objectMapper);
    }

    ActionAuditLog toDomain(ObjectMapper objectMapper) {
        return new ActionAuditLog(
                id,
                clusterId,
                namespace,
                targetKind,
                targetName,
                actionType,
                actor,
                actorRole,
                status,
                risk,
                approvalRequired,
                approvalId,
                objectMapper.convertValue(parameters, STRING_MAP),
                objectMapper.convertValue(beforeState, OBJECT_MAP),
                objectMapper.convertValue(afterState, OBJECT_MAP),
                objectMapper.convertValue(diff, DIFF_LIST),
                message,
                createdAt,
                executedAt
        );
    }
}
