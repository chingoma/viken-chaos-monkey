INSERT INTO chaos_scheduler_config (id, uid, name, cron_expression, enabled, experiment_type, target_services, default_blast_radius, environment, created_by)
VALUES (
    '01900000-0000-7000-8000-000000000031',
    '01900000-0000-7000-8000-000000000031',
    'dev-hourly-pod-kill',
    '0 0 * * * *',
    FALSE,
    'pod-kill',
    '["order-service"]'::jsonb,
    5,
    'DEV',
    'system'
);
