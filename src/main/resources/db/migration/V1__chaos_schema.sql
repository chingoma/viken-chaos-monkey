-- Viken Chaos Monkey — enterprise schema (UUIDv7 uid + UUID PK)

CREATE TABLE chaos_environment_policy (
    id              UUID PRIMARY KEY,
    uid             VARCHAR(36) NOT NULL UNIQUE,
    environment     VARCHAR(32) NOT NULL UNIQUE,
    allow_real_adapters BOOLEAN NOT NULL DEFAULT FALSE,
    allow_scheduler BOOLEAN NOT NULL DEFAULT FALSE,
    require_approval  BOOLEAN NOT NULL DEFAULT TRUE,
    max_daily_experiments INTEGER NOT NULL DEFAULT 10,
    allowed_namespaces JSONB NOT NULL DEFAULT '[]'::jsonb,
    updated_by      VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE chaos_safety_policy (
    id              UUID PRIMARY KEY,
    uid             VARCHAR(36) NOT NULL UNIQUE,
    environment     VARCHAR(32) NOT NULL UNIQUE,
    chaos_enabled   BOOLEAN NOT NULL DEFAULT FALSE,
    kill_switch_active BOOLEAN NOT NULL DEFAULT TRUE,
    blast_radius_max_percent INTEGER NOT NULL DEFAULT 10,
    excluded_services JSONB NOT NULL DEFAULT '[]'::jsonb,
    enabled_types   JSONB NOT NULL DEFAULT '[]'::jsonb,
    window_start    TIME,
    window_end      TIME,
    updated_by      VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE chaos_experiment (
    id              UUID PRIMARY KEY,
    uid             VARCHAR(36) NOT NULL UNIQUE,
    type            VARCHAR(64) NOT NULL,
    target_service  VARCHAR(128) NOT NULL,
    namespace       VARCHAR(128),
    params_json     JSONB NOT NULL DEFAULT '{}'::jsonb,
    status          VARCHAR(32) NOT NULL,
    requested_by    VARCHAR(128),
    approved_by     VARCHAR(128),
    executed_by     VARCHAR(128),
    blast_radius_percent INTEGER NOT NULL DEFAULT 1,
    environment     VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    result_message  TEXT,
    correlation_id  VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_chaos_experiment_status ON chaos_experiment(status);
CREATE INDEX idx_chaos_experiment_created ON chaos_experiment(created_at DESC);

CREATE TABLE chaos_experiment_run (
    id              UUID PRIMARY KEY,
    uid             VARCHAR(36) NOT NULL UNIQUE,
    experiment_id   UUID NOT NULL REFERENCES chaos_experiment(id),
    run_number      INTEGER NOT NULL,
    status          VARCHAR(32) NOT NULL,
    adapter_used    VARCHAR(64),
    duration_ms     BIGINT,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    error_detail    TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chaos_experiment_audit (
    id              UUID PRIMARY KEY,
    uid             VARCHAR(36) NOT NULL UNIQUE,
    experiment_id   UUID REFERENCES chaos_experiment(id),
    action          VARCHAR(64) NOT NULL,
    actor           VARCHAR(128),
    request_id      VARCHAR(64),
    payload_json    JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chaos_audit_occurred ON chaos_experiment_audit(occurred_at DESC);

CREATE TABLE chaos_scheduler_config (
    id              UUID PRIMARY KEY,
    uid             VARCHAR(36) NOT NULL UNIQUE,
    name            VARCHAR(128) NOT NULL UNIQUE,
    cron_expression VARCHAR(64) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    experiment_type VARCHAR(64) NOT NULL DEFAULT 'pod-kill',
    target_services JSONB NOT NULL DEFAULT '[]'::jsonb,
    default_blast_radius INTEGER NOT NULL DEFAULT 5,
    environment     VARCHAR(32) NOT NULL DEFAULT 'DEV',
    last_run_at     TIMESTAMPTZ,
    next_run_at     TIMESTAMPTZ,
    last_status     VARCHAR(32),
    created_by      VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE TABLE chaos_adapter_config (
    id              UUID PRIMARY KEY,
    uid             VARCHAR(36) NOT NULL UNIQUE,
    adapter_type    VARCHAR(64) NOT NULL,
    environment     VARCHAR(32) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    config_json     JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_by      VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    UNIQUE(adapter_type, environment)
);

CREATE TABLE chaos_approval_request (
    id              UUID PRIMARY KEY,
    uid             VARCHAR(36) NOT NULL UNIQUE,
    experiment_id   UUID NOT NULL REFERENCES chaos_experiment(id),
    status          VARCHAR(32) NOT NULL,
    maker_id        VARCHAR(128) NOT NULL,
    checker_id      VARCHAR(128),
    maker_note      TEXT,
    checker_note    TEXT,
    requested_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ
);

-- Seed defaults
INSERT INTO chaos_environment_policy (id, uid, environment, allow_real_adapters, allow_scheduler, require_approval, max_daily_experiments, allowed_namespaces, updated_by)
VALUES
    ('01900000-0000-7000-8000-000000000001', '01900000-0000-7000-8000-000000000001', 'LOCAL', TRUE, TRUE, FALSE, 100, '["default","chaos-engineering"]'::jsonb, 'system'),
    ('01900000-0000-7000-8000-000000000002', '01900000-0000-7000-8000-000000000002', 'DEV', TRUE, TRUE, FALSE, 50, '["chaos-engineering"]'::jsonb, 'system'),
    ('01900000-0000-7000-8000-000000000003', '01900000-0000-7000-8000-000000000003', 'UAT', TRUE, FALSE, TRUE, 10, '["chaos-engineering"]'::jsonb, 'system'),
    ('01900000-0000-7000-8000-000000000004', '01900000-0000-7000-8000-000000000004', 'PROD', FALSE, FALSE, TRUE, 0, '[]'::jsonb, 'system');

INSERT INTO chaos_safety_policy (id, uid, environment, chaos_enabled, kill_switch_active, blast_radius_max_percent, excluded_services, enabled_types, updated_by)
VALUES
    ('01900000-0000-7000-8000-000000000011', '01900000-0000-7000-8000-000000000011', 'LOCAL', FALSE, TRUE, 10, '["payment-service","auth-service"]'::jsonb, '[]'::jsonb, 'system'),
    ('01900000-0000-7000-8000-000000000012', '01900000-0000-7000-8000-000000000012', 'DEV', FALSE, TRUE, 10, '["payment-service","auth-service"]'::jsonb, '[]'::jsonb, 'system'),
    ('01900000-0000-7000-8000-000000000013', '01900000-0000-7000-8000-000000000013', 'UAT', FALSE, TRUE, 5, '["payment-service","auth-service","gateway","iam-service"]'::jsonb, '[]'::jsonb, 'system'),
    ('01900000-0000-7000-8000-000000000014', '01900000-0000-7000-8000-000000000014', 'PROD', FALSE, TRUE, 1, '["payment-service","auth-service","gateway","iam-service","order-management-service","fix-middleware"]'::jsonb, '[]'::jsonb, 'system');

INSERT INTO chaos_adapter_config (id, uid, adapter_type, environment, enabled, config_json, updated_by)
VALUES
    ('01900000-0000-7000-8000-000000000021', '01900000-0000-7000-8000-000000000021', 'kubernetes-pod-kill', 'DEV', FALSE, '{"namespace":"chaos-engineering","labelSelector":"app"}'::jsonb, 'system'),
    ('01900000-0000-7000-8000-000000000022', '01900000-0000-7000-8000-000000000022', 'http-latency', 'DEV', FALSE, '{"allowedHosts":[]}'::jsonb, 'system'),
    ('01900000-0000-7000-8000-000000000023', '01900000-0000-7000-8000-000000000023', 'kafka-delay', 'DEV', FALSE, '{"allowedTopics":[]}'::jsonb, 'system');
