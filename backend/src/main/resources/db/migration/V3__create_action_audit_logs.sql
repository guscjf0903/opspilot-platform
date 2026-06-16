CREATE TABLE action_audit_logs (
    id UUID PRIMARY KEY,
    cluster_id VARCHAR(80) NOT NULL,
    namespace VARCHAR(120) NOT NULL,
    target_kind VARCHAR(40) NOT NULL,
    target_name VARCHAR(180) NOT NULL,
    action_type VARCHAR(60) NOT NULL,
    actor VARCHAR(180) NOT NULL,
    actor_role VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    risk VARCHAR(20) NOT NULL,
    approval_required BOOLEAN NOT NULL,
    approval_id UUID,
    parameters JSONB NOT NULL,
    before_state JSONB NOT NULL,
    after_state JSONB NOT NULL,
    diff JSONB NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    executed_at TIMESTAMPTZ
);

CREATE INDEX idx_action_audit_logs_cluster_created
    ON action_audit_logs (
        cluster_id,
        created_at
    );

CREATE INDEX idx_action_audit_logs_target_created
    ON action_audit_logs (
        cluster_id,
        namespace,
        target_kind,
        target_name,
        created_at
    );

CREATE TABLE action_approval_requests (
    id UUID PRIMARY KEY,
    audit_log_id UUID NOT NULL,
    cluster_id VARCHAR(80) NOT NULL,
    namespace VARCHAR(120) NOT NULL,
    target_kind VARCHAR(40) NOT NULL,
    target_name VARCHAR(180) NOT NULL,
    action_type VARCHAR(60) NOT NULL,
    requester VARCHAR(180) NOT NULL,
    requester_role VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    decided_by VARCHAR(180),
    decided_at TIMESTAMPTZ,
    decision_reason TEXT,
    CONSTRAINT fk_action_approval_audit
        FOREIGN KEY (audit_log_id)
        REFERENCES action_audit_logs (id)
);

CREATE INDEX idx_action_approvals_cluster_status
    ON action_approval_requests (
        cluster_id,
        status,
        requested_at
    );
