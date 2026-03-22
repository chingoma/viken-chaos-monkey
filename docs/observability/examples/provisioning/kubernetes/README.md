# Kubernetes Auto-Provisioning Example (Grafana)

This example mounts datasource and dashboard provisioning files into Grafana using ConfigMaps and a Deployment patch.

## What you get
- Automatic datasource provisioning at startup.
- Automatic dashboard provisioning from JSON files.
- Folder alignment with the observability strategy:
  - `00-Global`, `01-Edge`, `02-Service-Catalog`, `03-Services`, `04-Kubernetes`, `05-Data-Platforms`, `06-Business`

## Files
- `kustomization.yaml`
- `grafana-deployment-patch.yaml`
- `grafana-env-secret.example.yaml`

## Usage
1. Copy and customize `grafana-env-secret.example.yaml` as `grafana-env-secret.yaml`.
2. Set namespace and deployment name in `kustomization.yaml` / `grafana-deployment-patch.yaml`.
3. Apply:
   - `kubectl apply -k docs/observability/examples/provisioning/kubernetes`

## Assumptions
- Grafana Deployment exists (name defaults to `grafana`).
- Grafana container name is `grafana`.
- Namespace defaults to `monitoring`.
- Dashboard files are sourced from `../../grafana/*.json`.

## Notes
- This is Kustomize-based and environment-friendly.
- Dashboard ConfigMap updates require pod restart unless your Grafana watches file changes for providers.
