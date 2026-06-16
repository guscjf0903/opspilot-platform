CREATE TABLE incident_analysis_reports (
    id UUID PRIMARY KEY,
    cluster_id VARCHAR(80) NOT NULL,
    namespace VARCHAR(120),
    target_kind VARCHAR(40) NOT NULL,
    target_name VARCHAR(180) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    summary TEXT NOT NULL,
    impact_scope JSONB NOT NULL,
    root_cause_candidates JSONB NOT NULL,
    evidence JSONB NOT NULL,
    recommendations JSONB NOT NULL,
    next_checks JSONB NOT NULL,
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(120),
    provider_response_id VARCHAR(160),
    input_tokens INTEGER NOT NULL DEFAULT 0,
    cached_input_tokens INTEGER NOT NULL DEFAULT 0,
    output_tokens INTEGER NOT NULL DEFAULT 0,
    reasoning_output_tokens INTEGER NOT NULL DEFAULT 0,
    total_tokens INTEGER NOT NULL DEFAULT 0,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_incident_analysis_reports_cluster_created
    ON incident_analysis_reports (
        cluster_id,
        created_at
    );

CREATE INDEX idx_incident_analysis_reports_target_created
    ON incident_analysis_reports (
        cluster_id,
        namespace,
        target_kind,
        target_name,
        created_at
    );
