# Observability Documentation Index

This folder contains the production-grade Grafana observability strategy and implementation artifacts.

## Documents
- [Grafana Dashboard Strategy](GRAFANA_DASHBOARD_STRATEGY.md)
- [Telemetry Contract](TELEMETRY_CONTRACT.md)
- [Dashboard Catalog](DASHBOARD_CATALOG.yaml)
- [Service Dashboard Template](SERVICE_DASHBOARD_TEMPLATE.md)
- [Runtime and Dependency Dashboards](RUNTIME_AND_DEPENDENCY_DASHBOARDS.md)
- [SLO and Alerting Standard](SLO_AND_ALERTING_STANDARD.md)
- [Release and Business Visibility](RELEASE_AND_BUSINESS_VISIBILITY.md)
- [PromQL Library](PROMQL_LIBRARY.md)
- [Prometheus Rule Examples](examples/prometheus-rules-observability.yaml)
- [Grafana Starter Pack](examples/grafana/README.md)
- [Grafana Provisioning Bundle](examples/provisioning/README.md)
- [Kubernetes Grafana Auto-Provisioning](examples/provisioning/kubernetes/README.md)
- [Helm Grafana Override Example](examples/provisioning/helm/README.md)

## Suggested rollout order
1. telemetry contract
2. L1/L2/L3 dashboard foundations
3. runtime/dependency dashboards
4. SLO and alerting
5. release and business journey visibility
