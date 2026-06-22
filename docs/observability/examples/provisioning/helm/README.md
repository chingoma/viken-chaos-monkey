# Helm Override Example (`kube-prometheus-stack`)

Use this when Grafana is managed by Helm and you want dashboard/datasource auto-loading with minimal manual steps.

## Files
- `values-observability.yaml` - Helm values override for Grafana sidecars
- `configmaps/kustomization.yaml` - Generates labeled ConfigMaps for dashboards and datasources

## Workflow
1. Create/update secret used by Grafana env expansion:
   - `docs/observability/examples/provisioning/kubernetes/grafana-env-secret.example.yaml`
2. Generate/apply ConfigMaps:
   - `kubectl apply -k docs/observability/examples/provisioning/helm/configmaps`
3. Upgrade Helm release:
   - `helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack -n monitoring -f docs/observability/examples/provisioning/helm/values-observability.yaml`

## What gets auto-provisioned
- Datasources via sidecar (`grafana_datasource=1`)
- Dashboards via sidecar (`grafana_dashboard=1`) grouped by folder annotation:
  - `00-Global`, `01-Edge`, `02-Service-Catalog`, `03-Services`, `04-Kubernetes`, `05-Data-Platforms`, `06-Business`

## Notes
- This pattern avoids baking JSON dashboards directly into Helm values.
- Keep dashboard JSON in `docs/observability/examples/grafana/` and regenerate ConfigMaps with kustomize.
