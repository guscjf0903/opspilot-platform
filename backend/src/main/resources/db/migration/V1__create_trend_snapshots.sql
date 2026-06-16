CREATE TABLE kubernetes_workload_usage_snapshots (
    id UUID PRIMARY KEY,
    cluster_id VARCHAR(80) NOT NULL,
    namespace VARCHAR(120) NOT NULL,
    workload_kind VARCHAR(40) NOT NULL,
    workload_name VARCHAR(180) NOT NULL,
    status VARCHAR(30) NOT NULL,
    desired_replicas INTEGER NOT NULL,
    available_replicas INTEGER NOT NULL,
    ready_replicas INTEGER NOT NULL,
    cpu_request_cores DOUBLE PRECISION,
    memory_request_bytes BIGINT,
    cpu_usage_avg_cores DOUBLE PRECISION,
    cpu_usage_p95_cores DOUBLE PRECISION,
    memory_usage_avg_bytes BIGINT,
    memory_usage_p95_bytes BIGINT,
    cpu_usage_percent DOUBLE PRECISION,
    memory_usage_percent DOUBLE PRECISION,
    metrics_available BOOLEAN NOT NULL,
    metrics_reason VARCHAR(120) NOT NULL,
    range_minutes INTEGER NOT NULL,
    collected_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_k8s_usage_snapshots_target_time
    ON kubernetes_workload_usage_snapshots (
        cluster_id,
        namespace,
        workload_kind,
        workload_name,
        collected_at
    );

CREATE TABLE cost_daily_snapshots (
    id UUID PRIMARY KEY,
    cluster_id VARCHAR(80) NOT NULL,
    namespace VARCHAR(120) NOT NULL,
    workload_kind VARCHAR(40) NOT NULL,
    workload_name VARCHAR(180) NOT NULL,
    status VARCHAR(30) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    estimated_daily_cost DOUBLE PRECISION NOT NULL,
    estimated_monthly_cost DOUBLE PRECISION NOT NULL,
    cpu_monthly_cost DOUBLE PRECISION NOT NULL,
    memory_monthly_cost DOUBLE PRECISION NOT NULL,
    estimated_monthly_saving DOUBLE PRECISION NOT NULL,
    estimation_mode VARCHAR(80) NOT NULL,
    snapshot_date DATE NOT NULL,
    collected_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_cost_daily_snapshots_target_date UNIQUE (
        cluster_id,
        namespace,
        workload_kind,
        workload_name,
        snapshot_date
    )
);

CREATE INDEX idx_cost_daily_snapshots_target_date
    ON cost_daily_snapshots (
        cluster_id,
        namespace,
        workload_kind,
        workload_name,
        snapshot_date
    );
