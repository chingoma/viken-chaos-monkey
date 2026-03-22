# Grafana Baseline Dashboard Starter Pack

This folder provides starter JSON dashboards aligned with the observability strategy.

## Included dashboards
- `l1-platform-global.json`
- `l2-service-catalog.json`
- `l3-service-health-template.json`
- `l3-api-gateway.json`
- `l3-k8s-workload.json`
- `l3-k8s-pod-runtime.json`
- `l3-k8s-node-infra.json`
- `l3-jvm-runtime.json`
- `l3-database.json`
- `l3-cache.json`
- `l3-messaging.json`
- `l3-logs-exceptions.json`
- `l3-tracing-dependencies.json`
- `l2-slo-error-budget.json`
- `l2-release-impact.json`
- `l3-auth-iam-security.json`
- `l2-business-journey.json`
- `dashboard-pack-manifest.yaml`

## Import instructions
1. In Grafana, open **Dashboards -> New -> Import**.
2. Upload each JSON file.
3. Bind your Prometheus, Loki, and Tempo datasources by name:
   - `Prometheus`
   - `Loki`
   - `Tempo`
4. Save dashboards into folders:
   - `00-Global`
   - `01-Edge`
   - `02-Service-Catalog`
   - `03-Services/<service-name>`
   - `04-Kubernetes`
   - `05-Data-Platforms`
   - `06-Business`

## Notes
- These are production-minded starters, not full final dashboards.
- Keep panel count intentionally low for triage.
- Extend using `SERVICE_DASHBOARD_TEMPLATE.md` and `PROMQL_LIBRARY.md`.
- For automated environment rollout, use `../provisioning/`.
