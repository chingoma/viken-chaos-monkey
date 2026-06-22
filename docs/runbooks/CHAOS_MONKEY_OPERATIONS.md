# Viken Chaos Monkey — Operations Runbook

## Emergency kill switch

1. **API:** `POST /api/chaos/emergency/disable` (ADMIN or `chaos:admin:kill-switch` permission)
2. **Verify:** `GET /api/chaos/status` → `killSwitchActive: true`
3. **Metric:** `chaos_kill_switch_active == 1` in Prometheus

## Break-glass prod pod-kill (extreme caution)

Requires explicit env flags on the deployment:

- `CHAOS_K8S_ENABLED=true`
- `CHAOS_K8S_PROD_OVERRIDE=true`
- Maker-checker approval for experiment in UAT/PROD (`chaos.environment=PROD`)

## Quarterly chaos drill

1. Enable chaos in DEV safety policy (`chaos_safety_policy.chaos_enabled=true`)
2. Run simulated experiments first (`chaos.k8s.enabled=false`)
3. Enable K8s adapter in `chaos-engineering` namespace only
4. Review audit table `chaos_experiment_audit` and Kafka topic `audit.chaos.experiments`
5. Deactivate kill switch and disable chaos after drill

## Incident response

| Symptom | Action |
|---------|--------|
| Unexpected pod restarts | Activate kill switch immediately |
| Scheduler running in prod | Disable `chaos_scheduler_config.enabled` in DB |
| Audit gap | Check Kafka consumer lag on `audit.chaos.experiments` |

## Dashboards

Import [`docs/observability/chaos-monkey-dashboard.json`](observability/chaos-monkey-dashboard.json) into Grafana.

Apply alert rules from [`docs/observability/chaos-monkey-alerts.yaml`](observability/chaos-monkey-alerts.yaml).
