# Grafana Provisioning Bundle

This bundle lets you bootstrap dashboards and datasources consistently across `dev`, `sit`, `uat`, and `prod`.

## Contents
- `grafana/provisioning/datasources/datasources-template.yaml`
- `grafana/provisioning/dashboards/providers.yaml`
- `import-dashboards.sh`
- `.env.example`
- `kubernetes/` (ConfigMap + Deployment patch for in-cluster auto-provisioning)
- `helm/` (kube-prometheus-stack values override + sidecar ConfigMap generation)

## What this supports
- Standard datasource aliases:
  - `DS_PROMETHEUS`
  - `DS_LOKI`
  - `DS_TEMPO`
- Folder-aware dashboard import from `docs/observability/examples/grafana`
- Idempotent import behavior by dashboard `uid`

## Prerequisites
- Grafana API key with admin rights for dashboard provisioning
- `curl` and `jq` installed
- Reachable Grafana URL

## Quick start
1. Copy `.env.example` to `.env` and update values.
2. Run:
   - `./import-dashboards.sh`
3. Verify imported dashboards in folders:
   - `00-Global`, `01-Edge`, `02-Service-Catalog`, `03-Services`, `04-Kubernetes`, `05-Data-Platforms`, `06-Business`

## Notes
- Datasource template is for file-based provisioning in Grafana containers.
- Import script uses the API and can be used in CI/CD jobs.
- For Kubernetes auto-mount and startup provisioning, use `kubernetes/README.md`.
- For Helm-managed Grafana (kube-prometheus-stack), use `helm/README.md`.
