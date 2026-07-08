-- Entities extending BaseEntity require created_at/updated_at; V1 omitted them on some tables.
ALTER TABLE chaos_approval_request
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

ALTER TABLE chaos_experiment_audit
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

ALTER TABLE chaos_experiment_run
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
